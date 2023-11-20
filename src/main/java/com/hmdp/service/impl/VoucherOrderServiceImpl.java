package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 加载lua脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 创建阻塞队列
     * 当一个线程尝试从队列中获取元素时,如果没有元素,那么这个线程就会被阻塞,直到队列中有元素,
     * 被阻塞的线程才会被唤醒,并且获取元素.
     */
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    /**
     * 线程池
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 执行线程池
     *
     * @PostConstruct表示当前类初始化完毕之后,就执行这个方法
     */
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 线程任务
     * 异步执行订单下单
     */
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //获取队列当前的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    /**
     * 代理对象放到当前类中
     */
    private IVoucherOrderService proxy;

    /**
     * 创建订单
     *
     * @param voucherOrder 队列当前的订单信息
     * @return
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //获取用户
        Long userId = voucherOrder.getUserId();
        //使用Redisson 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            //获取失败,返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            //由于spring的事务是放在threadLocal中, 此时是多线程,事务会失效
            log.debug("createVoucherOrder..................");
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            //释放锁
            lock.unlock();
        }
    }

    /**
     * 优惠券秒杀
     *
     * @param voucherId 用户正在抢购的优惠券id
     * @return 订单id
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );

        //判断结果是否有购买资格,也就是是否为0
        // 0 下单成功 1 库存不足 2 重复下单
        int r = result.intValue();
        if (r != 0) {
            //不为0,没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        //为0,有购买资格.  下单信息保存到,阻塞队列中
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单id
        voucherOrder.setId(orderId);
        //用户id
        voucherOrder.setUserId(userId);
        //优惠券id
        voucherOrder.setVoucherId(voucherId);
        //放入阻塞队列中
        orderTasks.add(voucherOrder);

        //获取代理对象(事务)
        proxy = (IVoucherOrderService)AopContext.currentProxy();
        //返回订单id
        return Result.ok(orderId);
    }

    /**
     * 创建订单 用悲观锁实现一人一单  乐观锁实现库存超卖
     *
     * @param voucherOrder 优惠券id
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //一人一单
        Long userId = UserHolder.getUser().getId();

        //查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        //判断是否存在
        if (count > 0) {
            //存在,至少下过一单
            log.error("只能购买一次!");
            return;
        }
        log.debug("--------------------------");
        //扣减库存 CAS解决超卖
        boolean isSuccess = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                .update();
        if (!isSuccess) {
            log.error("库存不足");
            return;
        }
        //创建订单
        save(voucherOrder);
    }
}
