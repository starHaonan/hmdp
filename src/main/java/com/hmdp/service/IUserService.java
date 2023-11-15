package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码,并且保存到session中
     *
     * @param phone   请求参数:手机号
     * @param session 用来保存登录状态
     * @return 登录成功或失败
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session session
     * @return 登录成功或失败
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
