package com.edgareldy.springboottutorial.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import java.util.List;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Customer}
 * returned by the API, never the JPA entity itself. The JSON contract is
 * snake_case, decoupled from the Java fields' own camelCase names via
 * explicit {@link JsonProperty} annotations. {@code products} (the
 * products this customer has ordered at least once) is only populated on
 * the single-customer detail endpoint ({@code GET /api/v1/customers/{id}}):
 * the paginated list endpoint, and the {@code customer} nested inside an
 * {@code OrderResponse}, both leave it as an empty list rather than
 * running an extra query per customer.
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
        List<ProductResponse> products
) {
}
