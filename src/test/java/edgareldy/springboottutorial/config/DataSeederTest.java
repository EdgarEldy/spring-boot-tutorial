package edgareldy.springboottutorial.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DataSeeder}, with {@link CategoryRepository} and
 * {@link ProductRepository} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void seedsCategoriesAndProductsWhenDatabaseIsEmpty() throws Exception {
        when(categoryRepository.count()).thenReturn(0L);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(Category.builder().id(1L).categoryName("Electronics").build())
                .thenReturn(Category.builder().id(2L).categoryName("Books").build());

        dataSeeder.seedDemoData().run();

        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(productRepository, times(3)).save(any(Product.class));
    }

    @Test
    void skipsSeedingWhenCategoriesAlreadyExist() throws Exception {
        when(categoryRepository.count()).thenReturn(1L);

        dataSeeder.seedDemoData().run();

        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }
}
