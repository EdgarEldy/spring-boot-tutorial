package com.edgareldy.springboottutorial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe configuration for the local file storage layer, bound from the
 * {@code app.storage} prefix in {@code application.yml}. Registered via
 * {@link org.springframework.boot.context.properties.EnableConfigurationProperties}
 * on {@link StorageConfig} so the binding is local to this feature without
 * requiring a package-level scan.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @DefaultValue("./uploads/products") String uploadDir,
        @DefaultValue("5") int maxFileSizeMb
) {
}
