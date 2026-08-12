package ru.yandex.practicum.order.logger;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {
    @Around("@annotation(ru.yandex.practicum.order.logger.Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("Entering method:  {}() - Arguments: {}", methodName, args);

        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long duration = System.currentTimeMillis() - startTime;

        log.info("← {}() - Completed in {}ms - Response: {}", methodName, duration, result);
        return result;
    }
}
