package com.edgareldy.springboottutorial.dto.order;

import java.util.List;

/**
 * One entry of {@code GET /api/v1/orders}: a customer, every product they
 * ordered (one line per order, with its quantity and total) and the sum of
 * those totals. Each customer appears once, however many orders they have.
 * <p>
 * Created edgar.muhamyangabo on 11/12/26
 * Author : edgar.muhamyangabo
 * Date : 11/12/26
 * Project : spring-boot-tutorial
 */
public record CustomerOrdersResponse(
        OrderCustomerResponse customer,
        List<OrderedProductResponse> products,
        double total
) {
}
