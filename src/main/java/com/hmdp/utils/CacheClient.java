package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @Author: lemme
 * @ClassName: CacheClient
 * @PackageName: com.hmdp.utils
 * @Description: 基于StringRedisTemplate封装的一个缓存工具类
 * 解决 缓存击穿,缓存穿透
 */
@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     *
     * @param key   传入redis key
     * @param value 传入redis value
     * @param time  设置key的过期时间
     * @param unit  设置key的过期时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，
     * 并且可以设置逻辑过期时间，用于处理缓存击穿问题
     *
     * @param key   传入redis key
     * @param value 传入redis value
     * @param time  设置key的过期时间
     * @param unit  设置key的过期时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        //设置过期时间:当前时间基础上,加传入的time
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        //写入redis
        //逻辑过期就不传时间了
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 查询信息
     * 解决缓存穿透(缓存未命中,存入redis中)
     * 根据指定的key查询缓存,并反序列化为指定类型,利用缓存空值的方式解决缓存穿透问题
     *
     * @param keyPreFix  存储到redis中的kye前缀
     * @param id         根据id(类型不确定)
     * @param type       返回值的类型(泛型推断)
     * @param dbFallback 谁调用谁传递逻辑
     *                   ( Function<ID, R> ID是参数类型,R是返回值类型  返回值的类型(泛型推断))
     * @param time       设置key的过期时间
     * @param unit       设置key的过期时间单位
     * @return R 返回值
     */
    public  <ID, R> R queryWithPassThrough(String keyPreFix, ID id, Class<R> type,
                                           Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPreFix + id;
        //从redis中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在,直接返回
            return JSONUtil.toBean(json, type);
        }

        //判断命中的是否是空值
        //如果redis中的值为空了,那就不要查询数据库了.直接返回
        if ("".equals(json)) {
            return null;
        }
        //不存在,根据id查询数据库
        R r = dbFallback.apply(id);

        //数据库中不存在,返回错误,没有此数据
        if (r == null) {
            //将空值写入redis(防止缓存穿透)
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在,写入redis.(缓存:方便下次直接用,不用查数据库)
        this.set(key, r, time, unit);
        return r;
    }
}
