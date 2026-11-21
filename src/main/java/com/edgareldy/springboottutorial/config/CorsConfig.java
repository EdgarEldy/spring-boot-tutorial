package com.edgareldy.springboottutorial.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * CORS configuration active only under the "dev" profile. Dynamically
 * allows any origin that is a local address: any frontend dev server on
 * localhost ({@code http://localhost:*}), the Android emulator host alias
 * ({@code http://10.0.2.2:*}), and physical mobile devices on the same
 * LAN ({@code http://192.168.*} and private {@code http://10.*} range).
 * <p>
 * Uses a {@link CorsConfigurationSource} bean so Spring Security's
 * {@code .cors()} picks it up directly instead of relying on the Spring MVC
 * {@code WebMvcConfigurer} path, which does not intercept pre-flight OPTIONS
 * requests before the security filter chain. In prod no bean of this type is
 * registered so the reverse proxy is responsible for CORS headers.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Configuration
@Profile("dev")
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            String origin = request.getHeader("Origin");
            if (origin == null || !isLocalOrigin(origin)) {
                return null;
            }
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(origin));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        };
    }

    private boolean isLocalOrigin(String origin) {
        return origin.startsWith("http://localhost:")
                || origin.startsWith("http://10.0.2.2:")
                || origin.startsWith("http://192.168.")
                || (origin.startsWith("http://10.") && !origin.startsWith("http://10.0.2.2:"));
    }
}
