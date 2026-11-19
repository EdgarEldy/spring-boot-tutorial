package com.edgareldy.springboottutorial.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Response DTO for a single cart item. The subtotal is computed by the
 * mapper as quantity * unitPrice and serialised as a JSON number.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public record CartItemResponse(
        Long id,

        @JsonProperty("product_id")
        Long productId,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("unit_price")
        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal subtotal
) {}
