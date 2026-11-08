package com.edgareldy.springboottutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} task execution, allowing cron-based
 * or fixed-rate tasks (such as the token cleanup scheduler) to be declared
 * with the {@link org.springframework.scheduling.annotation.Scheduled}
 * annotation on any Spring bean.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
