package com.edgareldy.springboottutorial.config;

import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.CategoryRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds demo categories, products, and an initial admin account on startup,
 * active only under the "dev" profile (same reasoning as {@code CorsConfig}:
 * this is throwaway demo data, not something prod or the
 * Testcontainers-backed test profile should ever see). Each block is guarded
 * by its own count check so restarting the app against a persistent dev
 * database does not duplicate rows.
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
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDemoData(
            @Value("${app.seed.reset-user-passwords:false}") boolean resetUserPasswords) {
        return seedDemoData(resetUserPasswords);
    }

    CommandLineRunner seedDemoData(boolean resetPasswords) {
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

            if (appUserRepository.count() == 0) {
                Role adminRole = roleRepository.findByRoleNameIgnoreCase("ADMIN")
                        .orElseThrow(() -> new ResourceNotFoundException("Bootstrap role ADMIN not found"));
                AppUser admin = AppUser.builder()
                        .firstName("Admin")
                        .lastName("User")
                        .username("admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .enabled(true)
                        .roles(Set.of(adminRole))
                        .build();
                appUserRepository.save(admin);
            }

            if (resetPasswords) {
                String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);
                List<AppUser> users = appUserRepository.findAll();
                users.forEach(user -> user.setPassword(encodedPassword));
                appUserRepository.saveAll(users);
            }
        };
    }
}
