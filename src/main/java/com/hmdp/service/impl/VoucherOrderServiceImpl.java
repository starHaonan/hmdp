package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 优惠券秒杀
     *
     * @param voucherId 优惠券id
     * @return 订单id
     */
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //未开始
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //已结束
            return Result.fail("秒杀已经结束");
        }
        //判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        //一人一单
        Long userId = UserHolder.getUser().getId();
        //查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //判断是否存在
        if (count > 0) {
            //存在,至少下过一单
            return Result.fail("已经购买过一次!");
        }

        //扣减库存 CAS解决超卖
        boolean isSuccess = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!isSuccess) {
            return Result.fail("库存不足");
        }

        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单id
        long orderId = redisIdWorker.nextId("order");
        System.out.println(orderId);
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        //优惠券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        //返回订单id
        return Result.ok(orderId);
    }
}
