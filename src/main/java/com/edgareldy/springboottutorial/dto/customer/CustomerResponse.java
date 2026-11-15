package com.edgareldy.springboottutorial.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import java.util.List;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Customer}
 * returned by the API, never the JPA entity itself. The JSON contract is
 * snake_case, decoupled from the Java fields' own camelCase names via
 * explicit {@link JsonProperty} annotations. {@code products} lists what
 * this customer ordered, one line per order with its quantity and total,
 * and {@code total} is the sum of those totals. Both are populated on the
 * paginated list and on the detail endpoint; a customer without orders has
 * an empty list and a total of 0.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CustomerResponse(
        Long id,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        String telephone,
        String email,
        String address,
        List<OrderedProductResponse> products,
        double total
) {
}
