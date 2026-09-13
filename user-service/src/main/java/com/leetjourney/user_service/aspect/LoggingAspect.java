package com.leetjourney.user_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.leetjourney.user_service.service.*.*(..))")
    public void serviceMethod(){}


    @Before("serviceMethod()")
    public void logBefore(JoinPoint joinPoint){

        log.info("Called service method : {} with arguments : {} ",
               joinPoint.getSignature().getName(),joinPoint.getArgs() );
    }

    @AfterReturning(pointcut ="serviceMethod()",returning="result")
    public void logAfter(JoinPoint joinPoint, Object result){
        log.info("Service method :{} , returned : {}",
                joinPoint.getSignature().getName(),result);

    }



}
