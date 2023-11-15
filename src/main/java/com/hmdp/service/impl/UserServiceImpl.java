package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送验证码,并且保存到session中
     *
     * @param phone   请求参数:手机号
     * @param session 用来保存登录状态
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        //TODO 开发时候,每次要输入测试麻烦(用utils下的正则校验)
        //2.生成随机验证码(调用hutool工具)
        String code = RandomUtil.randomNumbers(6);
        //3.保存验证码到session
        session.setAttribute("code",code);
        //4.发送验证码.(这里模拟一下)
        log.debug("验证码发送成功,验证码是:{}",code);
        return Result.ok();
    }
}
