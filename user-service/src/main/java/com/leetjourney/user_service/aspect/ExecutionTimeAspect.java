package com.leetjourney.user_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Pointcut("execution(* com.leetjourney.user_service.controller.*.*(..))")
    public void controllerMethods(){

    }

    @Around("controllerMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
          return  joinPoint.proceed();
        }finally{
            long end = System.currentTimeMillis();
            long elapsedNs=end-start;
            long elpasedMs= TimeUnit.NANOSECONDS.toMillis(elapsedNs);
            String signature=joinPoint.getSignature().toShortString();
            log.info("Controller method {} executed in {} ms", signature,elpasedMs);
        }
    }
}
