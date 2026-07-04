package edgareldy.springboottutorial.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Measures and logs how long each controller method takes to handle an
 * HTTP request, demonstrating a second, independent {@code @Around} advice
 * alongside {@link LoggingAspect}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Aspect
@Component
public class ExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("execution(* edgareldy.springboottutorial.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMillis = System.currentTimeMillis() - start;
            log.info("{} executed in {} ms", joinPoint.getSignature().toShortString(), elapsedMillis);
        }
    }
}
