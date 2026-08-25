package edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * {@code @DataJpaTest} for {@link ProductRepository}, backed by a real
 * PostgreSQL instance via Testcontainers.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Category electronics;
    private Category furniture;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(Category.builder().categoryName("Electronics").build());
        furniture = categoryRepository.save(Category.builder().categoryName("Furniture").build());
        productRepository.save(Product.builder()
                .category(electronics).productName("Keyboard").unitPrice(79.99f).build());
        productRepository.save(Product.builder()
                .category(furniture).productName("Desk").unitPrice(199.99f).build());
    }

    @Test
    void findAllEagerlyLoadsCategoryForEveryProduct() {
        var page = productRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        for (Product product : page.getContent()) {
            boolean categoryLoaded = entityManager.getEntityManagerFactory()
                    .getPersistenceUnitUtil()
                    .isLoaded(product, "category");
            assertThat(categoryLoaded).as("category should be eagerly fetched by @EntityGraph").isTrue();
        }
    }

    @Test
    void findByCategoryIdReturnsOnlyMatchingProducts() {
        var page = productRepository.findByCategoryId(electronics.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Product::getProductName).containsExactly("Keyboard");
    }

    @Test
    void findByIdWithCategoryEagerlyLoadsCategory() {
        Long productId = productRepository.findByCategoryId(furniture.getId(), PageRequest.of(0, 10))
                .getContent().get(0).getId();

        var found = productRepository.findByIdWithCategory(productId);

        assertThat(found).isPresent();
        assertThat(found.get().getCategory().getCategoryName()).isEqualTo("Furniture");
    }

    @Test
    void savingStaleProductThrowsOptimisticLockingFailure() {
        Product saved = productRepository.saveAndFlush(
                Product.builder().category(electronics).productName("Mouse").unitPrice(29.99f).build());

        // Simulates a concurrent update from another transaction: bumps the row's
        // version directly in the database, bypassing this test's persistence
        // context so `saved` keeps its now-stale in-memory version.
        entityManager.createNativeQuery("UPDATE products SET version = version + 1 WHERE id = :id")
                .setParameter("id", saved.getId())
                .executeUpdate();

        saved.setUnitPrice(19.99f);
        assertThatThrownBy(() -> productRepository.saveAndFlush(saved))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void existsByCategoryIdReflectsCurrentData() {
        assertThat(productRepository.existsByCategoryId(electronics.getId())).isTrue();

        Category empty = categoryRepository.save(Category.builder().categoryName("Empty").build());

        assertThat(productRepository.existsByCategoryId(empty.getId())).isFalse();
    }
}
