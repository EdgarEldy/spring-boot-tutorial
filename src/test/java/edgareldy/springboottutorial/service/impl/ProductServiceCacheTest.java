package edgareldy.springboottutorial.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.dto.product.ProductRequest;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.mapper.ProductMapper;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import edgareldy.springboottutorial.service.ProductService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the {@code @Cacheable}/{@code @CacheEvict} behavior on
 * {@link ProductServiceImpl}, same approach and reasoning as
 * {@link CategoryServiceCacheTest}: a local {@link ConcurrentMapCacheManager}
 * rather than the Caffeine {@code CacheManager} from {@code CacheConfig}, a
 * feature/core-architecture type this branch does not depend on.
 * {@code @SpringBootTest} reuses the same context, and therefore the same
 * cache, across every test method in this class, so each test clears it
 * first to avoid one test's cached entry leaking into another.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = {ProductServiceImpl.class, ProductServiceCacheTest.TestCacheConfig.class})
class ProductServiceCacheTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("products");
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductMapper productMapper;

    @BeforeEach
    void clearCache() {
        Cache products = cacheManager.getCache("products");
        if (products != null) {
            products.clear();
        }
    }

    private static Product sampleProduct() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        return Product.builder().id(1L).category(category).productName("Keyboard").unitPrice(79.99f).build();
    }

    @Test
    void findByIdIsCachedAcrossCalls() {
        Product product = sampleProduct();
        ProductResponse response = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.findById(1L);
        productService.findById(1L);

        verify(productRepository, times(1)).findByIdWithCategory(1L);
    }

    @Test
    void updateEvictsCacheForThatId() {
        Product product = sampleProduct();
        ProductResponse response = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(product.getCategory()));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.findById(1L);
        productService.update(1L, new ProductRequest(1L, "Mechanical Keyboard", 99.99f));
        productService.findById(1L);

        verify(productRepository, times(2)).findByIdWithCategory(1L);
    }

    @Test
    void deleteEvictsCacheForThatId() {
        Product product = sampleProduct();
        ProductResponse response = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.findById(1L);
        productService.delete(1L);
        productService.findById(1L);

        verify(productRepository, times(2)).findByIdWithCategory(1L);
    }
}
