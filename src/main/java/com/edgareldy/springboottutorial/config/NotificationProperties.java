package com.edgareldy.springboottutorial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized configuration for notification settings. Bound from the
 * {@code app.notifications} prefix so each property can be set via
 * environment variable (e.g. {@code APP_NOTIFICATION_ADMIN_EMAIL}) without
 * recompiling. Registered in {@link AsyncConfig}.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        @DefaultValue("admin@example.com") String adminEmail
) {
}
