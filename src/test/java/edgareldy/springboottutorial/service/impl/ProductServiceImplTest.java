package edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.product.ProductRequest;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.mapper.ProductMapper;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link ProductServiceImpl}, with {@link ProductRepository},
 * {@link CategoryRepository}, and {@link ProductMapper} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).categoryName("Electronics").build();
        product = Product.builder().id(1L).category(category).productName("Keyboard").unitPrice(79.99f).build();
        productResponse = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
    }

    @Test
    void findAllWithoutCategoryIdUsesPlainFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        PageResponse<ProductResponse> result = productService.findAll(null, pageable);

        assertThat(result.content()).containsExactly(productResponse);
    }

    @Test
    void findAllWithCategoryIdFiltersByCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByCategoryId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        PageResponse<ProductResponse> result = productService.findAll(1L, pageable);

        assertThat(result.content()).containsExactly(productResponse);
        verify(productRepository, never()).findAll(pageable);
    }

    @Test
    void findByIdReturnsResponseWhenFound() {
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        assertThat(productService.findById(1L)).isEqualTo(productResponse);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createResolvesCategoryAndSaves() {
        ProductRequest request = new ProductRequest(1L, "Keyboard", 79.99f);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        assertThat(productService.create(request)).isEqualTo(productResponse);
    }

    @Test
    void createThrowsWhenCategoryMissing() {
        ProductRequest request = new ProductRequest(99L, "Keyboard", 79.99f);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteRemovesProductWhenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }
}
