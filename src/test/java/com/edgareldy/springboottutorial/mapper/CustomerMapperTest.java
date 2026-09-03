package com.edgareldy.springboottutorial.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests the real MapStruct-generated {@link CustomerMapperImpl}, not a mock:
 * {@link CustomerMapper#toResponse} and {@link CustomerMapper#toDetailResponse}
 * differ only in how they treat {@code products}, so a test exercising the
 * generated code is the only way to catch a regression where one method's
 * behavior leaks into the other.
 * <p>
 * Created edgar.muhamyangabo on 9/3/26
 * Author : edgar.muhamyangabo
 * Date : 9/3/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(classes = CustomerMapperImpl.class)
class CustomerMapperTest {

    @Autowired
    private CustomerMapper customerMapper;

    private Customer customer() {
        return Customer.builder().id(1L).firstName("Ada").lastName("Lovelace")
                .telephone("+1 202-555-0100").email("ada@example.com").address("1 Analytical Engine Way").build();
    }

    @Test
    void toResponseAlwaysReturnsEmptyProducts() {
        CustomerResponse response = customerMapper.toResponse(customer());

        assertThat(response.products()).isEmpty();
    }

    @Test
    void toDetailResponseMapsSuppliedProducts() {
        ProductResponse keyboard = new ProductResponse(10L, "Keyboard", 79.99f, 1L, "Electronics");

        CustomerResponse response = customerMapper.toDetailResponse(customer(), List.of(keyboard));

        assertThat(response.products()).containsExactly(keyboard);
    }
}
