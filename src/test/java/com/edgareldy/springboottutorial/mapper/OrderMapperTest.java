package com.edgareldy.springboottutorial.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderItemResponse;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests the real MapStruct-generated {@link OrderMapperImpl} together with
 * {@link OrderItemMapperImpl} and {@link ProductMapperImpl}: verifies that
 * entity-to-DTO mapping, enum-to-String conversion for {@code status}, the
 * manual {@code toResponse(OrderProjection)} overload, and the grouped-view
 * helper {@code toCustomerOrdersResponse} all produce correct output.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = {OrderMapperImpl.class, OrderItemMapperImpl.class, ProductMapperImpl.class})
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void _01_ShouldMapOrderEntityToResponse_WithStatusStringAndItems() {
        Category cat = Category.builder().id(1L).categoryName("Electronics").build();
        Product product = Product.builder().id(2L).productName("Keyboard")
                .unitPrice(new BigDecimal("50.00")).category(cat).build();
        Customer customer = Customer.builder().id(3L).firstName("Ada").lastName("Lovelace")
                .telephone("+1 202-555-0100").email("ada@example.com")
                .address("1 Analytical Engine Way").build();

        OrderItem item = OrderItem.builder()
                .id(10L).product(product).productName("Keyboard")
                .unitPrice(new BigDecimal("50.00")).quantity(2)
                .subtotal(new BigDecimal("100.00")).build();
        Order order = Order.builder()
                .id(5L).customer(customer).product(product).quantity(2)
                .totalAmount(new BigDecimal("100.00")).orderNumber("ORD-TEST-001")
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);

        OrderResponse response = orderMapper.toResponse(order);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.customer().firstName()).isEqualTo("Ada");
        assertThat(response.product().productName()).isEqualTo("Keyboard");
        assertThat(response.items()).hasSize(1);
        OrderItemResponse itemResponse = response.items().get(0);
        assertThat(itemResponse.id()).isEqualTo(10L);
        assertThat(itemResponse.productId()).isEqualTo(2L);
        assertThat(itemResponse.productName()).isEqualTo("Keyboard");
        assertThat(itemResponse.unitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(itemResponse.quantity()).isEqualTo(2);
        assertThat(itemResponse.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void _02_ShouldMapProjectionToResponse_WithNullDescriptionAndActiveFromProjection() {
        OrderProjection projection = new OrderProjection(
                7L, 3L, "Grace", "Hopper", "+1 202-555-0101", "grace@example.com", "2 Compiler Street",
                1L, "Desk", new BigDecimal("200.00"), true, 2L, "Office",
                1, new BigDecimal("200.00"), OrderStatus.CONFIRMED, "ORD-TEST-007");

        OrderResponse response = orderMapper.toResponse(projection);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-007");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.customer().firstName()).isEqualTo("Grace");
        assertThat(response.product().productName()).isEqualTo("Desk");
        assertThat(response.product().description()).isNull();
        assertThat(response.product().active()).isTrue();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void _03_ShouldBuildGroupedResponse_WithOrderedProductLines() {
        Customer customer = Customer.builder().id(1L).firstName("Ada").lastName("Lovelace")
                .telephone("+1 202-555-0100").email("ada@example.com")
                .address("1 Analytical Engine Way").build();
        List<OrderProjection> projections = List.of(
                new OrderProjection(1L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com",
                        "1 Analytical Engine Way", 2L, "Keyboard", new BigDecimal("50.00"), true,
                        3L, "Electronics", 2, new BigDecimal("100.00"), OrderStatus.PENDING, "ORD-TEST-001"),
                new OrderProjection(2L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com",
                        "1 Analytical Engine Way", 4L, "Desk", new BigDecimal("200.00"), true,
                        3L, "Electronics", 1, new BigDecimal("200.00"), OrderStatus.PENDING, "ORD-TEST-002"));

        CustomerOrdersResponse response = orderMapper.toCustomerOrdersResponse(
                customer, projections, new BigDecimal("300.00"));

        assertThat(response.customer().firstName()).isEqualTo("Ada");
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.products()).hasSize(2);
        List<OrderedProductResponse> lines = response.products();
        assertThat(lines.get(0).productName()).isEqualTo("Keyboard");
        assertThat(lines.get(0).quantity()).isEqualTo(2);
        assertThat(lines.get(0).total()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(lines.get(1).productName()).isEqualTo("Desk");
        assertThat(lines.get(1).total()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
