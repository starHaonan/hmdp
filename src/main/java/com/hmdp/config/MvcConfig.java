package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Author: lemme
 * @ClassName: MvcConfig
 * @PackageName: com.hmdp.config
 * @Description: 让 LoginInterceptor(拦截器)生效
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //让拦截器生效

        //登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                //排除不拦截的路径
                .excludePathPatterns(
                        //拦截部分请求
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                //order设置优先级, 数字越小越先执行
                ).order(1);
        //token刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                //拦截所有请求
                .addPathPatterns("/**").order(0);
    }
}
