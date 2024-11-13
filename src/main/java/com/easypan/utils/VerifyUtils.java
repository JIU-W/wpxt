package com.easypan.utils;


import com.easypan.entity.enums.VerifyRegexEnum;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerifyUtils {

    public static boolean verify(String regex, String value) {
        if (StringTools.isEmpty(value)) {
            return false;
        }
        //如果字符串不为空，就使用正则表达式 regex 创建一个 Pattern 对象。
        //Pattern.compile(regex) 将正则表达式编译为一个模式。
        Pattern pattern = Pattern.compile(regex);
        //使用 Pattern对象创建一个 Matcher 对象，用于在输入字符串中执行匹配操作。
        //pattern.matcher(value) 返回一个匹配器，该匹配器会尝试将正则表达式与输入字符串 value 进行匹配。
        Matcher matcher = pattern.matcher(value);
        //最后，通过调用 matcher.matches() 方法，判断整个输入字符串是否完全匹配正则表达式。
        //如果匹配成功，返回 true，表示验证通过；否则返回 false，表示验证不通过。
        return matcher.matches();
    }

    public static boolean verify(VerifyRegexEnum regex, String value) {
        return verify(regex.getRegex(), value);
    }

    public static void main(String[] args) {
        System.out.println(new File("E:\\代码生成\\..\\workspace-java").exists());

    }

}

