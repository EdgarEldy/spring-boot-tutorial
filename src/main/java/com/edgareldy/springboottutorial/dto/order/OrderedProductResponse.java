package com.edgareldy.springboottutorial.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One order of a customer, seen as "a product they ordered": the product,
 * the ordered quantity and the order total. {@code orderId} is kept so the
 * caller can still update or delete that specific order.
 * <p>
 * Created edgar.muhamyangabo on 11/12/26
 * Author : edgar.muhamyangabo
 * Date : 11/12/26
 * Project : spring-boot-tutorial
 */
public record OrderedProductResponse(
        @JsonProperty("order_id")
        Long orderId,
        @JsonProperty("product_id")
        Long productId,
        @JsonProperty("product_name")
        String productName,
        @JsonProperty("unit_price")
        float unitPrice,
        int quantity,
        double total
) {
}
