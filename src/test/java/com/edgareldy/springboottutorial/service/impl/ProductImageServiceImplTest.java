package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.ProductImage;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.ProductImageMapper;
import com.edgareldy.springboottutorial.repository.ProductImageRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.storage.FileStorageService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link ProductImageServiceImpl}, with all dependencies mocked.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ProductImageMapper productImageMapper;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    private Product product;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).productName("Keyboard").build();
        file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());
    }

    @Test
    void _01_ShouldAddImage_WhenProductExists() {
        ProductImage saved = ProductImage.builder().id(10L).product(product)
                .imageUrl("/uploads/products/uuid.jpg").primary(false).displayOrder(0).build();
        ProductImageResponse response = new ProductImageResponse(10L, "/uploads/products/uuid.jpg", null, false, 0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.store(file, "products")).thenReturn("/uploads/products/uuid.jpg");
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(response);

        ProductImageResponse result = productImageService.addImage(1L, file, null, false);

        assertThat(result).isEqualTo(response);
        verify(fileStorageService).store(file, "products");
    }

    @Test
    void _02_ShouldThrowNotFound_WhenProductMissingOnAdd() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.addImage(99L, file, null, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void _03_ShouldUnsetOldPrimary_WhenAddingNewPrimaryImage() {
        ProductImage existingPrimary = ProductImage.builder().id(5L).product(product)
                .imageUrl("/uploads/products/old.jpg").primary(true).displayOrder(0).build();
        ProductImage saved = ProductImage.builder().id(10L).product(product)
                .imageUrl("/uploads/products/new.jpg").primary(true).displayOrder(0).build();
        ProductImageResponse response = new ProductImageResponse(10L, "/uploads/products/new.jpg", null, true, 0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdAndPrimaryTrue(1L))
                .thenReturn(Optional.of(existingPrimary));
        when(fileStorageService.store(file, "products")).thenReturn("/uploads/products/new.jpg");
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(saved);
        when(productImageMapper.toResponse(saved)).thenReturn(response);

        productImageService.addImage(1L, file, null, true);

        assertThat(existingPrimary.isPrimary()).isFalse();
        verify(productImageRepository).save(existingPrimary);
    }

    @Test
    void _04_ShouldDeleteImage_WhenImageBelongsToProduct() {
        ProductImage image = ProductImage.builder().id(10L).product(product)
                .imageUrl("/uploads/products/uuid.jpg").build();

        when(productImageRepository.findById(10L)).thenReturn(Optional.of(image));

        productImageService.deleteImage(1L, 10L);

        verify(fileStorageService).delete("/uploads/products/uuid.jpg");
        verify(productImageRepository).delete(image);
    }

    @Test
    void _05_ShouldThrowNotFound_WhenImageMissingOnDelete() {
        when(productImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteImage(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void _06_ShouldThrowNotFound_WhenImageDoesNotBelongToProduct() {
        Product otherProduct = Product.builder().id(99L).productName("Mouse").build();
        ProductImage image = ProductImage.builder().id(10L).product(otherProduct)
                .imageUrl("/uploads/products/uuid.jpg").build();

        when(productImageRepository.findById(10L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> productImageService.deleteImage(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).delete(any());
    }
}
