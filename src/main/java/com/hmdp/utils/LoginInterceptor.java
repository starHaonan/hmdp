package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Author: lemme
 * @ClassName: LoginInterceptor
 * @PackageName: com.hmdp.utils
 * @Description: 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * controller方法执行前执行
     * 返回true 程序继续向下执行  不拦截
     * 返回false 程序不往下执行  就是拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否要拦截,根据ThreadLoad中是否有用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            //没有, 需要拦截,设置状态码
            response.setStatus(401);
            //拦截
            return false;
        }

        //有用户,放行
        return true;
    }
}
