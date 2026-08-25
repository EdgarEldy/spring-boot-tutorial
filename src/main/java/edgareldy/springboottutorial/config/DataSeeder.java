package edgareldy.springboottutorial.config;

import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
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

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner seedDemoData() {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }

            Category electronics = categoryRepository.save(Category.builder().categoryName("Electronics").build());
            Category books = categoryRepository.save(Category.builder().categoryName("Books").build());

            productRepository.save(Product.builder()
                    .category(electronics).productName("Mechanical Keyboard").unitPrice(79.99f).build());
            productRepository.save(Product.builder()
                    .category(electronics).productName("Wireless Mouse").unitPrice(29.99f).build());
            productRepository.save(Product.builder()
                    .category(books).productName("Effective Java").unitPrice(45.50f).build());
        };
    }
}
