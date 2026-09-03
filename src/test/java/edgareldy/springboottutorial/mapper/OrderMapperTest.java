package edgareldy.springboottutorial.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import edgareldy.springboottutorial.dto.order.OrderResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Customer;
import edgareldy.springboottutorial.entity.Order;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.repository.OrderProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests the real MapStruct-generated {@link OrderMapperImpl}, not a mock.
 * {@link OrderMapper#toResponse(Order)} delegates to {@link CustomerMapper}/
 * {@link ProductMapper} (via {@code uses}), while
 * {@link OrderMapper#toResponse(OrderProjection)} is a hand-written
 * {@code default} method rebuilding the same DTOs field by field from a flat
 * projection; only a test against the generated code can catch a silent
 * field transposition (e.g. first/last name swapped) in either path.
 * <p>
 * Created edgar.muhamyangabo on 9/3/26
 * Author : edgar.muhamyangabo
 * Date : 9/3/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = {OrderMapperImpl.class, CustomerMapperImpl.class, ProductMapperImpl.class})
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void toResponseFromEntityNestsFullCustomerAndProductWithEmptyProductsList() {
        Customer customer = Customer.builder().id(1L).firstName("Ada").lastName("Lovelace")
                .telephone("+1 202-555-0100").email("ada@example.com").address("1 Analytical Engine Way").build();
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        Product product = Product.builder().id(10L).category(category).productName("Keyboard").unitPrice(50.0f).build();
        Order order = Order.builder().id(100L).customer(customer).product(product).quantity(2).total(100.0).build();

        OrderResponse response = orderMapper.toResponse(order);

        assertThat(response.customer().firstName()).isEqualTo("Ada");
        assertThat(response.customer().lastName()).isEqualTo("Lovelace");
        assertThat(response.customer().products()).isEmpty();
        assertThat(response.product().productName()).isEqualTo("Keyboard");
        assertThat(response.product().categoryName()).isEqualTo("Electronics");
    }

    @Test
    void toResponseFromProjectionMapsEachFieldToItsOwnDtoProperty() {
        OrderProjection projection = new OrderProjection(
                100L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                10L, "Keyboard", 50.0f, 5L, "Electronics", 2, 100.0);

        OrderResponse response = orderMapper.toResponse(projection);

        assertThat(response.customer().id()).isEqualTo(1L);
        assertThat(response.customer().firstName()).isEqualTo("Ada");
        assertThat(response.customer().lastName()).isEqualTo("Lovelace");
        assertThat(response.customer().telephone()).isEqualTo("+1 202-555-0100");
        assertThat(response.customer().email()).isEqualTo("ada@example.com");
        assertThat(response.customer().address()).isEqualTo("1 Analytical Engine Way");
        assertThat(response.customer().products()).isEmpty();
        assertThat(response.product().id()).isEqualTo(10L);
        assertThat(response.product().productName()).isEqualTo("Keyboard");
        assertThat(response.product().unitPrice()).isEqualTo(50.0f);
        assertThat(response.product().categoryId()).isEqualTo(5L);
        assertThat(response.product().categoryName()).isEqualTo("Electronics");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.total()).isEqualTo(100.0);
    }
}
