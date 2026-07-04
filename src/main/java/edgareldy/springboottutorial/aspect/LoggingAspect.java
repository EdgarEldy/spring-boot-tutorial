package edgareldy.springboottutorial.aspect;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs entry, exit, arguments, and thrown exceptions for every method on
 * every {@code @Service} bean, demonstrating {@code @Around} advice kept
 * entirely outside the business logic it observes.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* edgareldy.springboottutorial.service..*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        log.info("Entering {} with arguments {}", signature, Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.info("Exiting {} with result {}", signature, result);
            return result;
        } catch (Throwable ex) {
            log.error("Exception in {}: {}", signature, ex.getMessage());
            throw ex;
        }
    }
}
