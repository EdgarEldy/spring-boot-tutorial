package com.edgareldy.springboottutorial.dto.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the authenticated user's shopping cart. The total is
 * the sum of all item subtotals and is computed by the mapper.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal total
) {}
