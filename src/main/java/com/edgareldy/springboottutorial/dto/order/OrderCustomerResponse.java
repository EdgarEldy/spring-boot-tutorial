package com.edgareldy.springboottutorial.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The customer as it appears inside an order response: identity and contact
 * details only. Unlike {@code CustomerResponse} it has no {@code products}
 * list, which was always empty when a customer was nested in an order.
 * <p>
 * Created edgar.muhamyangabo on 11/12/26
 * Author : edgar.muhamyangabo
 * Date : 11/12/26
 * Project : spring-boot-tutorial
 */
public record OrderCustomerResponse(
        Long id,
        @JsonProperty("first_name")
        String firstName,
        @JsonProperty("last_name")
        String lastName,
        String telephone,
        String email,
        String address
) {
}
