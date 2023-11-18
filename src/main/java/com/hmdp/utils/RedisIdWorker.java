package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author: lemme
 * @ClassName: RedisIdWorker
 * @PackageName: com.hmdp.utils
 * @Description: Redis实现全局唯一id
 */
@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 开始时间戳 初始时间 2022-06-15 00:00:00
     */
    private static final long BEGIN_TIMESTAMP = 1655251200L;
    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    /**
     * 时间戳+序列号
     *
     * @param keyPrefix key的前缀
     * @return 全局唯一id
     */
    public long nextId(String keyPrefix) {
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        //生成序列号
        //获取当前日期 精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyy:MM:dd"));
        //自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
