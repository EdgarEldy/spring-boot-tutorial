package edgareldy.springboottutorial.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the {@code jwt.*} properties ({@code jwt.secret},
 * {@code jwt.expiration-ms}), replacing two separate {@code @Value}
 * injections in {@link JwtService}. Registered via
 * {@code @EnableConfigurationProperties} on {@code SecurityConfig} rather
 * than {@code @ConfigurationPropertiesScan} on the application class, to
 * keep this branch's JWT-specific wiring self-contained in its own files.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {
}
