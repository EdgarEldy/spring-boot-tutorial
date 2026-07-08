package edgareldy.springboottutorial.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.mapper.CategoryMapper;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import edgareldy.springboottutorial.service.CategoryService;
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
 * {@link CategoryServiceImpl}, using a plain in-memory
 * {@link ConcurrentMapCacheManager} rather than the Caffeine
 * {@code CacheManager} from {@code CacheConfig} (a feature/core-architecture
 * type this branch does not depend on): the point is to check the caching
 * contract Spring applies from the annotations, not which concrete cache
 * backend is wired in production. {@code @SpringBootTest} reuses the same
 * context, and therefore the same cache, across every test method in this
 * class, so each test clears it first to avoid one test's cached entry
 * leaking into another.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = {CategoryServiceImpl.class, CategoryServiceCacheTest.TestCacheConfig.class})
class CategoryServiceCacheTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("categories");
        }
    }

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @BeforeEach
    void clearCache() {
        Cache categories = cacheManager.getCache("categories");
        if (categories != null) {
            categories.clear();
        }
    }

    @Test
    void findByIdIsCachedAcrossCalls() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        categoryService.findById(1L);
        categoryService.findById(1L);

        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    void updateEvictsCacheForThatId() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.findById(1L);
        categoryService.update(1L, new CategoryRequest("Home Appliances"));
        categoryService.findById(1L);

        // 3, not 2: update() loads the entity via getCategoryOrThrow(), which
        // calls categoryRepository.findById() directly rather than through
        // the cached findById() service method, so that lookup is never
        // served from cache. The 3rd call is the one that actually proves
        // eviction: without it, this last findById(1L) would still be a
        // cache hit and the total would stay at 2.
        verify(categoryRepository, times(3)).findById(1L);
    }

    @Test
    void deleteEvictsCacheForThatId() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.findById(1L);
        categoryService.delete(1L);
        categoryService.findById(1L);

        // Same reasoning as updateEvictsCacheForThatId: delete() also loads
        // the entity via the uncached getCategoryOrThrow().
        verify(categoryRepository, times(3)).findById(1L);
    }
}
