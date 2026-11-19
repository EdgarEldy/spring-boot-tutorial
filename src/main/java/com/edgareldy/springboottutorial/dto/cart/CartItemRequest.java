package com.edgareldy.springboottutorial.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload to add a product to the authenticated user's cart.
 * No userId field: the user is always resolved from the SecurityContext.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public record CartItemRequest(
        @NotNull(message = "Product ID must not be null")
        Long productId,

        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {}
