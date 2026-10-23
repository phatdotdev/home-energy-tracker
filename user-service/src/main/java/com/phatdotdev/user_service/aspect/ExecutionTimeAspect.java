package com.phatdotdev.user_service.aspect;

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

    @Pointcut("execution(*  com.phatdotdev.user_service.controller.*.*(..))")
    public void controllerMethods(){}

    @Around("controllerMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp){
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            long end = System.nanoTime();
            long elapseNs = end - start;
            long elapseMs = TimeUnit.NANOSECONDS.toMillis(elapseNs);
            String signature = pjp.getSignature().toShortString();
            log.info("Controller method {} executed in {} ms", signature, elapseMs);
        }
    }
}
