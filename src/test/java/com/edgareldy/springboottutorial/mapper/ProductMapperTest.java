package com.edgareldy.springboottutorial.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.dto.product.ProductRequest;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests the real MapStruct-generated {@link ProductMapperImpl}, not a mock:
 * the service tests mock the mapper, so only this class can prove that
 * {@link ProductMapper#toEntity} and {@link ProductMapper#updateEntityFromRequest}
 * really assign the category handed in by the service, and never the
 * identity or the version. Also verifies that {@code active} is not overwritten
 * by write mappings, since {@link ProductRequest} carries no active field.
 * <p>
 * Created edgar.muhamyangabo on 9/21/26
 * Author : edgar.muhamyangabo
 * Date : 9/21/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = ProductMapperImpl.class)
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    void _01_ShouldAssignRequestFieldsAndCategory_WhenBuildingNewProduct() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        ProductRequest request = new ProductRequest(1L, "Keyboard", "A mechanical keyboard", new BigDecimal("79.99"));

        Product product = productMapper.toEntity(request, category);

        assertThat(product.getId()).isNull();
        assertThat(product.getVersion()).isNull();
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getProductName()).isEqualTo("Keyboard");
        assertThat(product.getDescription()).isEqualTo("A mechanical keyboard");
        assertThat(product.getUnitPrice()).isEqualByComparingTo(new BigDecimal("79.99"));
        // active is ignored on toEntity; the @Builder.Default gives it true
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void _02_ShouldKeepIdentityAndReplaceCategory_WhenUpdatingExistingProduct() {
        Category oldCategory = Category.builder().id(1L).categoryName("Electronics").build();
        Category newCategory = Category.builder().id(2L).categoryName("Office").build();
        Product existing = Product.builder().id(10L).version(3L).category(oldCategory)
                .productName("Keyboard").unitPrice(new BigDecimal("79.99")).active(false).build();

        productMapper.updateEntityFromRequest(
                new ProductRequest(2L, "Mechanical Keyboard", null, new BigDecimal("99.99")),
                newCategory, existing);

        assertThat(existing.getId()).isEqualTo(10L);
        assertThat(existing.getVersion()).isEqualTo(3L);
        assertThat(existing.getCategory()).isSameAs(newCategory);
        assertThat(existing.getProductName()).isEqualTo("Mechanical Keyboard");
        assertThat(existing.getUnitPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        // active must NOT be overwritten by updateEntityFromRequest
        assertThat(existing.isActive()).isFalse();
    }
}
