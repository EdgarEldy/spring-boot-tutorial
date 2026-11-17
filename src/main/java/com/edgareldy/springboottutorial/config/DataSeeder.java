package com.edgareldy.springboottutorial.config;

import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.repository.CategoryRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds a handful of categories and products on startup, active only under
 * the "dev" profile (same reasoning as {@code CorsConfig}: this is
 * throwaway demo data, not something prod or the Testcontainers-backed test
 * profile should ever see). Skips seeding entirely if any category already
 * exists, so restarting the app against a persistent dev database does not
 * duplicate rows.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    static final String DEMO_PASSWORD = "Admin1234!";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner seedDemoData() {
        return args -> {
            if (categoryRepository.count() == 0) {
                Category electronics = categoryRepository.save(Category.builder().categoryName("Electronics").build());
                Category books = categoryRepository.save(Category.builder().categoryName("Books").build());

                productRepository.save(Product.builder()
                        .category(electronics).productName("Mechanical Keyboard").unitPrice(new BigDecimal("79.99")).build());
                productRepository.save(Product.builder()
                        .category(electronics).productName("Wireless Mouse").unitPrice(new BigDecimal("29.99")).build());
                productRepository.save(Product.builder()
                        .category(books).productName("Effective Java").unitPrice(new BigDecimal("45.50")).build());
            }
        };
    }
}
