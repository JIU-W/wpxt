package com.easypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;
                                            //ElementType.TYPE指AOP拦截是用在类上面。
@Target({ElementType.METHOD, ElementType.TYPE})//ElementType.METHOD指AOP拦截是用在接口方法上面。我们主要是用这个
@Retention(RetentionPolicy.RUNTIME)//生命周期
//@Documented//可加可不加
@Mapping
public @interface GlobalInterceptor {

    /**
     * 校验登录：是否需要登录
     *
     * @return
     */
    //上传文件是要求要登录，所以要校验
    //查看别人分享的文件就不需要登录，所以就不用校验
    boolean checkLogin() default true;

    /**
     * 校验参数：是否需要校验参数
     *
     * @return
     */
    boolean checkParams() default false;//默认是不需要校验(故默认为false)，因为大部分操作是不用校验的。

    /**
     * 校验管理员
     *
     * @return
     */
    boolean checkAdmin() default false;

}