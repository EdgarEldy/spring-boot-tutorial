package com.edgareldy.springboottutorial.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link LoggingAspect}, asserting on the actual log output
 * (captured via a Logback {@link ListAppender}) rather than on private
 * masking methods directly.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
class LoggingAspectTest {

    private final LoggingAspect loggingAspect = new LoggingAspect();

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logbackLogger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
    }

    @Test
    void _01_ShouldMaskArgument_WhenParameterNameIsSensitive() throws Throwable {
        ProceedingJoinPoint joinPoint = fakeJoinPoint(
                "validate", new Class<?>[] {String.class}, new Object[] {"raw-secret-token"}, "unused-result");

        loggingAspect.logServiceCall(joinPoint);

        assertThat(loggedMessages()).noneMatch(message -> message.contains("raw-secret-token"));
        assertThat(loggedMessages()).anyMatch(message -> message.contains("***"));
    }

    @Test
    void _02_ShouldMaskReturnValue_WhenGenerateIsCalledOnATokenService() throws Throwable {
        ProceedingJoinPoint joinPoint = fakeJoinPoint(
                "generate", new Class<?>[] {String.class}, new Object[] {"someUser"}, "raw-generated-token");

        loggingAspect.logServiceCall(joinPoint);

        assertThat(loggedMessages()).noneMatch(message -> message.contains("raw-generated-token"));
    }

    @Test
    void _03_ShouldNotMaskAnything_WhenArgumentsAndReturnValuesAreNotSensitive() throws Throwable {
        ProceedingJoinPoint joinPoint = fakeJoinPoint(FakeGenericService.class,
                "describe", new Class<?>[] {String.class}, new Object[] {"plain-value"}, "plain-result");

        loggingAspect.logServiceCall(joinPoint);

        assertThat(loggedMessages()).anyMatch(message -> message.contains("plain-value"));
        assertThat(loggedMessages()).anyMatch(message -> message.contains("plain-result"));
    }

    private List<String> loggedMessages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private ProceedingJoinPoint fakeJoinPoint(
            String methodName, Class<?>[] paramTypes, Object[] args, Object returnValue) throws Throwable {
        return fakeJoinPoint(FakeActivationTokenServiceImpl.class, methodName, paramTypes, args, returnValue);
    }

    private ProceedingJoinPoint fakeJoinPoint(
            Class<?> target, String methodName, Class<?>[] paramTypes, Object[] args, Object returnValue)
            throws Throwable {
        Method method = target.getMethod(methodName, paramTypes);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toShortString()).thenReturn(method.getName());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);
        return joinPoint;
    }

    /**
     * Stand-in target whose name mirrors a real {@code *TokenServiceImpl},
     * exercising the aspect's declaring-class-name heuristic for return-value masking.
     * <p>
     * Created edgar.muhamyangabo on 10/30/26
     * Author : edgar.muhamyangabo
     * Date : 10/30/26
     * Project : spring-boot-tutorial
     */
    static class FakeActivationTokenServiceImpl {

        public String generate(String user) {
            return "unused";
        }

        public String validate(String token) {
            return "unused";
        }
    }

    /**
     * Stand-in target with no "Token" in its name, for the negative case.
     * <p>
     * Created edgar.muhamyangabo on 10/30/26
     * Author : edgar.muhamyangabo
     * Date : 10/30/26
     * Project : spring-boot-tutorial
     */
    static class FakeGenericService {

        public String describe(String label) {
            return "unused";
        }
    }
}
