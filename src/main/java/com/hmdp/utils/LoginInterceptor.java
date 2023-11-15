package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
        //获取session
        HttpSession session = request.getSession();

        //获取session中的用户
        Object user = session.getAttribute("user");

        //判断用户是否存在
        if (user == null) {
            //用不不存在 拦截 并返回401状态码
            response.setStatus(401);
            return false;
        }
        //存在,保存信息到ThreadLocal中
        UserHolder.saveUser((UserDTO) user);
        //放行
        return true;
    }


    /**
     * 请求和响应都完成了之后执行
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
