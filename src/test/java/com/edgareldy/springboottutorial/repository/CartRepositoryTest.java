package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springboottutorial.config.JpaAuditingConfig;
import com.edgareldy.springboottutorial.entity.Cart;
import com.edgareldy.springboottutorial.entity.CartItem;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@code @DataJpaTest} for {@link CartRepository} and {@link CartItemRepository},
 * backed by a real PostgreSQL instance via Testcontainers. Verifies the unique
 * constraints and the EntityGraph-based eager loading.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RepositoryTestcontainersConfiguration.class, JpaAuditingConfig.class})
class CartRepositoryTest {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager entityManager;

    private AppUser user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = appUserRepository.save(AppUser.builder()
                .username("alice_cart")
                .email("alice_cart@example.com")
                .password("{noop}Password1@")
                .enabled(true)
                .build());
        Category cat = categoryRepository.save(
                Category.builder().categoryName("Tech").build());
        product = productRepository.save(Product.builder()
                .productName("Mouse").unitPrice(new BigDecimal("29.99"))
                .category(cat).active(true).build());
    }

    @Test
    void _01_ShouldFindCartByUserId() {
        Cart cart = cartRepository.save(Cart.builder().user(user).build());

        assertThat(cartRepository.findByUserId(user.getId()))
                .isPresent()
                .get()
                .extracting(Cart::getId)
                .isEqualTo(cart.getId());
    }

    @Test
    void _02_ShouldFindCartByUsername() {
        cartRepository.save(Cart.builder().user(user).build());

        assertThat(cartRepository.findByUserUsername("alice_cart"))
                .isPresent();
    }

    @Test
    void _03_ShouldEnforceUniqueCartPerUser() {
        cartRepository.save(Cart.builder().user(user).build());
        entityManager.flush();

        // IDENTITY strategy triggers an immediate INSERT on save(), so the unique
        // constraint violation is thrown there, not deferred to entityManager.flush()
        Cart duplicate = Cart.builder().user(user).build();
        assertThatThrownBy(() -> cartRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _04_ShouldEnforceUniqueCartItemPerProduct() {
        Cart cart = cartRepository.save(Cart.builder().user(user).build());
        CartItem item1 = CartItem.builder().cart(cart).product(product).quantity(1).build();
        cartItemRepository.save(item1);
        entityManager.flush();

        // IDENTITY strategy triggers an immediate INSERT on save(), so the unique
        // constraint violation is thrown there, not deferred to entityManager.flush()
        CartItem duplicate = CartItem.builder().cart(cart).product(product).quantity(2).build();
        assertThatThrownBy(() -> cartItemRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _05_ShouldFindCartItemByCartIdAndProductId() {
        Cart cart = cartRepository.save(Cart.builder().user(user).build());
        CartItem item = CartItem.builder().cart(cart).product(product).quantity(3).build();
        cartItemRepository.save(item);

        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isPresent()
                .get()
                .extracting(CartItem::getQuantity)
                .isEqualTo(3);
    }
}
