package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 查询商户信息
     *
     * @param id 根据id
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class,
                id2 -> getById(id2), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //用互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        //返回
        return Result.ok(shop);
    }

    /**
     * 查询商户信息
     * 缓存击穿(用互斥锁)
     *
     * @param id 根据id
     */
    private Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在,直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //判断命中的是否是空值
        //如果redis中的值为空了,那就不要查询数据库了.直接返回
        if ("".equals(shopJson)) {
            return null;
        }

        //实现缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + "id";
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //判断是否获取成功
            if (!isLock) {
                //获取互斥锁失败,则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            //doubleCheck获取锁成功,再次检测redis缓存是否存在,如果存在则无需重建缓存
            String doubleCheck = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(doubleCheck)) {
                return JSONUtil.toBean(doubleCheck, Shop.class);
            }
            if ("".equals(shopJson)) {
                return null;
            }

            //获取互斥锁成功(不存在,根据id查询数据库)
            shop = getById(id);

            //因为是在本地,查询数据库速度很快.模拟一下 高延迟场景
            //Thread.sleep(200);

            //数据库中不存在,返回错误,没有此数据
            if (shop == null) {
                //将空值写入redis(防止缓存穿透)
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //存在,写入redis.(缓存:方便下次直接用,不用查数据库)
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }
        //返回
        return shop;
    }

    /**
     * 查询商户信息
     * 解决缓存穿透(缓存未命中,存入redis中)
     *
     * @param id 根据id
     */
    private Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在,直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //判断命中的是否是空值
        //如果redis中的值为空了,那就不要查询数据库了.直接返回
        if ("".equals(shopJson)) {
            return null;
        }
        //不存在,根据id查询数据库
        Shop shop = getById(id);
        //数据库中不存在,返回错误,没有此数据
        if (shop == null) {
            //将空值写入redis(防止缓存穿透)
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在,写入redis.(缓存:方便下次直接用,不用查数据库)
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    //获取互斥锁 (解决缓存击穿)
    private boolean tryLock(String key) {
        //利用redis的setnx方法来表示获取锁
        Boolean isHas = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        //因为isHas是Boolean包装类, 做拆箱时,有空指针风险,转换成基本类型
        return BooleanUtil.isTrue(isHas);
    }

    //释放互斥锁
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 更新商户(保证缓存一致性)
     * 当数据更新,删除缓存(redis中的数据)
     *
     * @param shop 要更新的商品信息
     * @return 成功或者失败
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空!");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        String key = CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(key);

        return Result.ok();
    }
}
