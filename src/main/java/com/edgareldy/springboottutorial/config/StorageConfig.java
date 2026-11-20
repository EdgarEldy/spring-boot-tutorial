package com.edgareldy.springboottutorial.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link StorageProperties} so that the {@code app.storage} prefix
 * in {@code application.yml} is bound and validated at startup. Keeping the
 * registration here rather than on the main application class limits the scope
 * of this feature to its own configuration class.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
