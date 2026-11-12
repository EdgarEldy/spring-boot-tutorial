package com.edgareldy.springboottutorial.dto.order;

import com.edgareldy.springboottutorial.dto.product.ProductResponse;

/**
 * Representation of a single
 * {@link com.edgareldy.springboottutorial.entity.Order} returned by the API
 * (detail, create, update, and the per-customer order list). Carries the
 * customer's contact details and the full {@link ProductResponse}, so a
 * caller never has to make a second request just to get the customer's
 * address or the product's category. The grouped view of
 * {@code GET /api/v1/orders} uses {@link CustomerOrdersResponse} instead.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderResponse(
        Long id,
        OrderCustomerResponse customer,
        ProductResponse product,
        int quantity,
        double total
) {
}
