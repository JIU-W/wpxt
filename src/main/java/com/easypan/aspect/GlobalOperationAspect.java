package com.easypan.aspect;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import com.easypan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
/**
 * 自定义切面
 */
@Component("operationAspect")
@Aspect
public class GlobalOperationAspect {

    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    //定义切入点
    @Pointcut("@annotation(com.easypan.annotation.GlobalInterceptor)")//加了GlobalInterceptor这个注解的才拦截
    private void requestInterceptor() {
    }

    /**
     * 前置通知
     */
    @Before("requestInterceptor()")       //JoinPoint: JoinPoint表示连接点，即被拦截的方法
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            //获取被拦截方法所属的目标对象
            Object target = point.getTarget();
            //获取被拦截方法的参数值
            Object[] arguments = point.getArgs();
            //获取被拦截方法的签名信息，包括方法名、返回类型、参数等信息。
            String methodName = point.getSignature().getName();//方法名

            //获取被拦截方法的参数类型
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            //通过反射获取方法对象
            //methodName是要获取的方法的名称，parameterTypes是方法的参数类型,使用getMethod()方法，可以获取与指定名称和参数类型匹配的方法。
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            //检查获取的方法是否有@GlobalInterceptor注解
            //如果方法有此注解，那么getAnnotation()方法将返回一个表示该注解的对象，否则返回null。
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (interceptor == null) {//基本不可能是null，因为只有加了GlobalInterceptor注解才会被拦截，才会检查。
                return;
            }
            /**
             * 校验登录和校验是否是管理员
             */
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            /**
             * 校验参数
             */
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }

        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }


    //校验登录
    private void checkLogin(Boolean checkAdmin) {
        //目的是获取当前的HTTP请求和会话对象，然后从会话中获取存储的用户信息。
        //这样做的目的是进行用户认证和状态管理。
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        //在用户未登录的情况下(即sessionUser为null),如果应用程序处于开发模式，
        //则通过查找用户信息并创建一个新的用户会话对象，将其存储到会话中以便后续使用。

        /*if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }*/

        //sessionUser为null为未登录的状态，抛出异常。
        if (sessionUser == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        //访问被拦截的接口 要校验是否是管理员 且 不是管理员，则说明此接口无法访问，报404错误即可
        if (checkAdmin && !sessionUser.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

    }

    /**
     * 校验参数
     * @param m
     * @param arguments
     * @throws BusinessException
     */
    private void validateParams(Method m, Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];//方法的参数
            Object value = arguments[i];//方法的参数的具体值(从前端得到的)

            //获取参数的注解
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {//因为有些参数有，而有些没有，若没有注解则跳过这个就找方法的下一个参数
                continue;
            }

            //基本数据类型
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) ||
                    TYPE_LONG.equals(parameter.getParameterizedType().getTypeName()) ||
                    TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            } else {
                //如果传递的是对象
                checkObjValue(parameter, value);
            }
        }
    }

    /**
     * 如果参数是包装在 实体类(dto) 里作为对象传过来就用这个校验
     * @param parameter
     * @param value
     */
    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 只是基本数据类型就用这个
     * @param value
     * @param verifyParam
     * @throws BusinessException
     */
    private void checkValue(Object value, VerifyParam verifyParam) throws BusinessException {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();

        /**
         * 校验空
         */
        if (isEmpty && verifyParam.required()) {
            //抛出异常
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        /**
         * 校验长度
         */
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        /**
         * 校验正则
         */
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

}