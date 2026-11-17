package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.config.JpaAuditingConfig;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
@Import({RepositoryTestcontainersConfiguration.class, JpaAuditingConfig.class})
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
                .category(category).productName("Keyboard").unitPrice(new BigDecimal("50.00")).build());
        desk = productRepository.save(Product.builder()
                .category(category).productName("Desk").unitPrice(new BigDecimal("200.00")).build());

        saveOrderWithItem(ada, keyboard, 2, new BigDecimal("100.00"), "ORD-TEST-001");
        saveOrderWithItem(grace, desk, 1, new BigDecimal("200.00"), "ORD-TEST-002");
    }

    @Test
    void _01_ShouldReturnEveryOrder_WhenNoFilterGiven() {
        var page = orderRepository.findAllProjected(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void _02_ShouldFilterByCustomer_WhenCustomerIdGiven() {
        var page = orderRepository.findAllProjected(ada.getId(), null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        OrderProjection projection = page.getContent().get(0);
        assertThat(projection.customerFirstName()).isEqualTo("Ada");
        assertThat(projection.customerLastName()).isEqualTo("Lovelace");
        assertThat(projection.productName()).isEqualTo("Keyboard");
        assertThat(projection.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void _03_ShouldFilterByProduct_WhenProductIdGiven() {
        var page = orderRepository.findAllProjected(null, desk.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).customerFirstName()).isEqualTo("Grace");
        assertThat(page.getContent().get(0).customerLastName()).isEqualTo("Hopper");
    }

    @Test
    void _03b_ShouldReturnEachCustomerOnce_WhenCustomerHasSeveralOrders() {
        saveOrderWithItem(ada, desk, 1, new BigDecimal("200.00"), "ORD-TEST-003");

        var page = orderRepository.findCustomersWithOrders(
                null, null, PageRequest.of(0, 10, Sort.by("id")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Customer::getFirstName).containsExactly("Ada", "Grace");
    }

    @Test
    void _03c_ShouldExcludeCustomersWithoutMatchingOrder_WhenPagingCustomers() {
        customerRepository.save(Customer.builder()
                .firstName("Alan").lastName("Turing").telephone("+1 202-555-0102")
                .email("alan@example.com").address("3 Enigma Road").build());

        var everyone = orderRepository.findCustomersWithOrders(null, null, PageRequest.of(0, 10));
        var deskBuyers = orderRepository.findCustomersWithOrders(null, desk.getId(), PageRequest.of(0, 10));
        var onlyAda = orderRepository.findCustomersWithOrders(ada.getId(), null, PageRequest.of(0, 10));

        assertThat(everyone.getContent()).extracting(Customer::getFirstName)
                .containsExactlyInAnyOrder("Ada", "Grace");
        assertThat(deskBuyers.getContent()).extracting(Customer::getFirstName).containsExactly("Grace");
        assertThat(onlyAda.getContent()).extracting(Customer::getFirstName).containsExactly("Ada");
    }

    @Test
    void _03d_ShouldLoadEveryOrderOfGivenCustomers_WhenProjectingByCustomerIds() {
        saveOrderWithItem(ada, desk, 3, new BigDecimal("600.00"), "ORD-TEST-003");

        List<OrderProjection> all = orderRepository.findAllProjectedByCustomerIds(
                List.of(ada.getId(), grace.getId()), null);
        List<OrderProjection> adaDeskOnly = orderRepository.findAllProjectedByCustomerIds(
                List.of(ada.getId()), desk.getId());

        assertThat(all).hasSize(3);
        assertThat(all).filteredOn(row -> row.customerId().equals(ada.getId()))
                .extracting(OrderProjection::productName).containsExactly("Keyboard", "Desk");
        assertThat(adaDeskOnly).hasSize(1);
        assertThat(adaDeskOnly.get(0).quantity()).isEqualTo(3);
        assertThat(adaDeskOnly.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void _04_ShouldLoadCustomerProductAndItems_WhenFindingOrderById() {
        Long orderId = orderRepository.findAllProjected(ada.getId(), null, PageRequest.of(0, 10))
                .getContent().get(0).id();

        var found = orderRepository.findByIdWithDetails(orderId);

        assertThat(found).isPresent();
        assertThat(found.get().getCustomer().getEmail()).isEqualTo("ada@example.com");
        assertThat(found.get().getProduct().getProductName()).isEqualTo("Keyboard");
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getProductName()).isEqualTo("Keyboard");
        assertThat(found.get().getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void _05_ShouldReturnTrue_WhenOrderExistsForCustomer() {
        assertThat(orderRepository.existsByCustomerId(ada.getId())).isTrue();
        assertThat(orderRepository.existsByCustomerId(ada.getId() + 999L)).isFalse();
    }

    @Test
    void _06_ShouldReturnTrue_WhenOrderExistsForProduct() {
        assertThat(orderRepository.existsByProductId(keyboard.getId())).isTrue();
        assertThat(orderRepository.existsByProductId(keyboard.getId() + 999L)).isFalse();
    }

    @Test
    void _07_ShouldReturnOrdersForCustomer_WhenPagingByCustomerId() {
        saveOrderWithItem(ada, desk, 1, new BigDecimal("200.00"), "ORD-TEST-ADA-2");

        var page = orderRepository.findByCustomerId(ada.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allSatisfy(o ->
                assertThat(o.getCustomer().getId()).isEqualTo(ada.getId()));
    }

    private Order saveOrderWithItem(Customer c, Product p, int qty, BigDecimal total, String orderNum) {
        BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(qty));
        OrderItem item = OrderItem.builder()
                .product(p).productName(p.getProductName())
                .unitPrice(p.getUnitPrice()).quantity(qty).subtotal(subtotal)
                .build();
        Order order = Order.builder()
                .customer(c).product(p).quantity(qty)
                .totalAmount(total).orderNumber(orderNum)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
        return orderRepository.save(order);
    }
}
