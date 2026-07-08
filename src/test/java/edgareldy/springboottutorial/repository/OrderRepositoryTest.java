package edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Customer;
import edgareldy.springboottutorial.entity.Order;
import edgareldy.springboottutorial.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * {@code @DataJpaTest} for {@link OrderRepository}, backed by a real
 * PostgreSQL instance via Testcontainers.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestcontainersConfiguration.class)
class OrderRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Customer ada;
    private Customer grace;
    private Product keyboard;
    private Product desk;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder().categoryName("Electronics").build());
        ada = customerRepository.save(Customer.builder()
                .firstName("Ada").lastName("Lovelace").telephone("+1 202-555-0100")
                .email("ada@example.com").address("1 Analytical Engine Way").build());
        grace = customerRepository.save(Customer.builder()
                .firstName("Grace").lastName("Hopper").telephone("+1 202-555-0101")
                .email("grace@example.com").address("2 Compiler Street").build());
        keyboard = productRepository.save(Product.builder()
                .category(category).productName("Keyboard").unitPrice(50.0f).build());
        desk = productRepository.save(Product.builder()
                .category(category).productName("Desk").unitPrice(200.0f).build());

        orderRepository.save(Order.builder().customer(ada).product(keyboard).quantity(2).total(100.0).build());
        orderRepository.save(Order.builder().customer(grace).product(desk).quantity(1).total(200.0).build());
    }

    @Test
    void findAllProjectedReturnsEveryOrderWhenNoFilter() {
        var page = orderRepository.findAllProjected(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void findAllProjectedFiltersByCustomerId() {
        var page = orderRepository.findAllProjected(ada.getId(), null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        OrderProjection projection = page.getContent().get(0);
        assertThat(projection.customerFullName()).isEqualTo("Ada Lovelace");
        assertThat(projection.productName()).isEqualTo("Keyboard");
        assertThat(projection.total()).isEqualTo(100.0);
    }

    @Test
    void findAllProjectedFiltersByProductId() {
        var page = orderRepository.findAllProjected(null, desk.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).customerFullName()).isEqualTo("Grace Hopper");
    }

    @Test
    void findByIdWithDetailsEagerlyLoadsCustomerAndProduct() {
        Long orderId = orderRepository.findAllProjected(ada.getId(), null, PageRequest.of(0, 10))
                .getContent().get(0).id();

        var found = orderRepository.findByIdWithDetails(orderId);

        assertThat(found).isPresent();
        assertThat(found.get().getCustomer().getEmail()).isEqualTo("ada@example.com");
        assertThat(found.get().getProduct().getProductName()).isEqualTo("Keyboard");
    }
}
