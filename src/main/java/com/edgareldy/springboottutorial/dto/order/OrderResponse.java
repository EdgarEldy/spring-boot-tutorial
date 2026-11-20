package com.edgareldy.springboottutorial.dto.order;

import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Representation of a single
 * {@link com.edgareldy.springboottutorial.entity.Order} returned by the API
 * (detail, create, update, and the per-customer order list). Carries the
 * customer's contact details and the full {@link ProductResponse} for backward
 * compatibility, plus the multi-line {@code items} breakdown and the
 * {@code totalAmount} computed by the service. The grouped view of
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

        @JsonProperty("order_number")
        String orderNumber,

        String status,

        @JsonProperty("total_amount")
        BigDecimal totalAmount,

        List<OrderItemResponse> items
) {
}
