package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: lemme
 * @ClassName: MvcConfig
 * @PackageName: com.hmdp.config
 * @Description: 让 LoginInterceptor(拦截器)生效
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //自己配置的拦截器
        LoginInterceptor loginInterceptor = new LoginInterceptor();
        //让拦截器生效
        registry.addInterceptor(loginInterceptor)
                //排除不拦截的路径
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                );
    }
}
