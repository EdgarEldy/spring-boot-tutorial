package edgareldy.springboottutorial.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction ({@code @Cacheable}/{@code @CacheEvict})
 * with an in-memory Caffeine {@link CacheManager}. The cache names declared
 * here ("categories", "products") are used by {@code feature/products},
 * which does not exist on this branch yet: this mirrors how
 * {@code OpenApiConfig} already declares the JWT bearer scheme ahead of
 * {@code feature/auth}, preparing the infrastructure a later branch needs
 * rather than waiting for it.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("categories", "products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500));
        return cacheManager;
    }
}
