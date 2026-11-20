package com.edgareldy.springboottutorial.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Representation of a single {@link com.edgareldy.springboottutorial.entity.OrderItem}
 * returned by the API. Carries the product snapshot (name and unit price at order
 * time) so callers see what was actually charged, independent of any subsequent
 * product update. The JSON contract uses snake_case via {@link JsonProperty}.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public record OrderItemResponse(
        Long id,

        @JsonProperty("product_id")
        Long productId,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("unit_price")
        BigDecimal unitPrice,

        int quantity,

        BigDecimal subtotal
) {
}
