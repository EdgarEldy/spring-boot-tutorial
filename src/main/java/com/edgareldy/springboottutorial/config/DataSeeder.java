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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDemoData() {
        return args -> {
            if (categoryRepository.count() == 0) {
                Category electronics = categoryRepository.save(Category.builder().categoryName("Electronics").build());
                Category books = categoryRepository.save(Category.builder().categoryName("Books").build());

                productRepository.save(Product.builder()
                        .category(electronics).productName("Mechanical Keyboard").unitPrice(79.99f).build());
                productRepository.save(Product.builder()
                        .category(electronics).productName("Wireless Mouse").unitPrice(29.99f).build());
                productRepository.save(Product.builder()
                        .category(books).productName("Effective Java").unitPrice(45.50f).build());
            }

            if (appUserRepository.count() == 0) {
                Role adminRole = roleRepository.findByRoleNameIgnoreCase("ADMIN")
                        .orElseThrow(() -> new ResourceNotFoundException("Bootstrap role ADMIN not found"));
                AppUser admin = AppUser.builder()
                        .username("admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("Admin1234!"))
                        .enabled(true)
                        .roles(Set.of(adminRole))
                        .build();
                appUserRepository.save(admin);
            }
        };
    }
}
