package com.edgareldy.springboottutorial.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload to update the quantity of an existing cart item.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public record CartItemUpdateRequest(
        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {}
