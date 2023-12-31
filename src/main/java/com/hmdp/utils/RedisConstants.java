package com.hmdp.utils;

public class RedisConstants {
    /**
     * 验证码的 key前缀
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    /**
     * 验证码失效时间
     */
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * token的前缀
     */
    public static final String LOGIN_USER_KEY = "login:token:";
    /**
     * 登录用户token的有效期
     */
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    /**
     * 商品缓存的key前缀
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    /**
     * 首页商品类型缓存的key前缀
     */
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop:type:";
    /**
     * 互斥锁id前缀
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    /**
     * 秒杀库存id前缀
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
