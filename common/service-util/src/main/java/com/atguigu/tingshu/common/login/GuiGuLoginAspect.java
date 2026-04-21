package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class GuiGuLoginAspect {

    @Autowired
    RedisTemplate redisTemplate;


    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object doBasicProfiling(ProceedingJoinPoint joinPoint, GuiGuLogin guiGuLogin) throws Throwable{
        Object result;

        try {
            //1.获取请求头中的token令牌
            //1.1 通过请求上下文对象获取请求对象 RequestAttributes接口 底层：基于ThreadLocal实现
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            if (servletRequestAttributes == null){
                throw new GuiguException(ResultCodeEnum.FAIL);
            }

            HttpServletRequest request = servletRequestAttributes.getRequest();
            String token = request.getHeader("token");
            if(token == null || token.isEmpty()){
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }

            //2.尝试查询Redis获取当前用户基本信息
            //2.1 构建查询Redis登录用户信息Key
            String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

            //2.2 调用Redis模板对象获取用户基本信息:UserInfoVo
            UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);

            //3.如果用户信息为空且目标方法要求必须登录 抛出异常：业务状态码设置208 前端引导用户跳转登录页
            if(userInfoVo == null){
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }

            //4.如果用户信息有值，将用户ID存入ThreadLocal，方便在javaEE三层controller，service，Mapper获取用户ID
            AuthContextHolder.setUserId(userInfoVo.getId());

            //5.执行目标方法
            result = joinPoint.proceed();

        } finally {
            //6.避免Threalocal出现内存泄漏，使用完毕清理ThreadLocal
            AuthContextHolder.removeUserId();
        }

        //7.响应结果
        return result;
    }



}
