package com.edgareldy.springboottutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA's native auditing support ({@code @CreatedDate}/
 * {@code @LastModifiedDate}, populated via {@code AuditingEntityListener}),
 * so entities can track creation/modification timestamps without
 * duplicating that logic by hand. Consumed by {@code Category} on
 * {@code feature/products}, the first entity to use it.
 * <p>
 * Created edgar.muhamyangabo on 8/25/26
 * Author : edgar.muhamyangabo
 * Date : 8/25/26
 * Project : spring-boot-tutorial
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
