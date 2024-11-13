package com.easypan.annotation;


import com.easypan.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//校验规则@Retention(RetentionPolicy.RUNTIME)//指定注解的生命周期。
//RetentionPolicy.RUNTIME   表示注解会在运行时保留，因此可以通过反射获取注解信息。

//ElementType.FIELD:表示注解可以应用于类的字段（成员变量）上
@Target({ElementType.PARAMETER, ElementType.FIELD})//ElementType.PARAMETER：表示注解可以应用于方法的参数上
public @interface VerifyParam {

    /**
     * 校验正则
     *
     * @return
     */
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;//默认为不用校验

    /**
     * 最小长度
     * @return
     */
    int min() default -1;//默认为-1

    /**
     * 最大长度
     * @return
     */
    int max() default -1;//默认为-1

    boolean required() default false;//默认为参数不是 “必传”的

}