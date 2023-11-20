package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Author: lemme
 * @ClassName: LoginInterceptor
 * @PackageName: com.hmdp.utils
 * @Description: 刷新token拦截器
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    /**
     * 因为这个类没有加入到容器中,所以StringRedisTemplate不能加入容器中.
     * 外界的StringRedisTemplate被注入,然后在赋值给本类的StringRedisTemplate,就可以用了
     */
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * controller方法执行前执行
     * 返回true 程序继续向下执行  不拦截
     * 返回false 程序不往下执行  就是拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token(在请求头中)
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //放行
            return true;
        }

        //基于token获取redis中的用户
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        //判断用户是否存在
        if (userMap.isEmpty()) {
            //放行
            return true;
        }

        //将查询到的hash数据转为UserDTO对象再存储
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //存在,保存信息到ThreadLocal中
        UserHolder.saveUser(userDTO);

        //刷新token有效期(如果用户一直在用,那有效期就重新计算)
        //TODO 测试麻烦,项目开发完成后记得打开.
        //stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }


    /**
     * 请求和响应都完成了之后执行
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
