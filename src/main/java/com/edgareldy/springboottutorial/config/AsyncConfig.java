package com.edgareldy.springboottutorial.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} and declares a dedicated thread pool for sending
 * emails (account activation, password reset), so a slow SMTP call never
 * blocks the request thread of the controller that triggered it.
 * <p>
 * Also registers a custom {@link AsyncUncaughtExceptionHandler}: an
 * exception thrown from a {@code void}-returning {@code @Async} method
 * (such as {@code EmailServiceImpl}'s send methods) has no caller to
 * propagate to, so without this handler it would only reach Spring's
 * default handler. The handler here logs the method name only, never the
 * raw arguments, since some of those (an activation or reset token) must
 * not be logged in plain text.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleUncaughtException;
    }

    private void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Unhandled exception in async method '{}': {}", method.getName(), ex.getMessage(), ex);
    }
}
