package edgareldy.springboottutorial.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests the real MapStruct-generated {@link CategoryMapperImpl}, not a mock:
 * {@link CategoryMapper#toResponse} and {@link CategoryMapper#toDetailResponse}
 * differ only in how they treat {@code products}, so a test exercising the
 * generated code is the only way to catch a regression where one method's
 * behavior leaks into the other.
 * <p>
 * Created edgar.muhamyangabo on 9/3/26
 * Author : edgar.muhamyangabo
 * Date : 9/3/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = {CategoryMapperImpl.class, ProductMapperImpl.class})
class CategoryMapperTest {

    @Autowired
    private CategoryMapper categoryMapper;

    private Category categoryWithProducts() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        Product keyboard = Product.builder().id(10L).category(category).productName("Keyboard").unitPrice(79.99f).build();
        category.setProducts(List.of(keyboard));
        return category;
    }

    @Test
    void toDetailResponseMapsProducts() {
        CategoryResponse response = categoryMapper.toDetailResponse(categoryWithProducts());

        assertThat(response.products()).extracting("productName").containsExactly("Keyboard");
    }

    @Test
    void toResponseAlwaysReturnsEmptyProductsEvenWhenLoaded() {
        CategoryResponse response = categoryMapper.toResponse(categoryWithProducts());

        assertThat(response.products()).isEmpty();
    }
}
