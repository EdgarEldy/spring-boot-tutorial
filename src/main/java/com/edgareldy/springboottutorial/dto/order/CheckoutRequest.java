package com.edgareldy.springboottutorial.dto.order;

/**
 * Request body for the checkout endpoint (POST /api/v1/orders). Intentionally
 * empty: the backend reads the authenticated user's cart, re-fetches prices
 * from the database, and calculates all amounts server-side. The client never
 * supplies ownership, prices or totals.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public record CheckoutRequest() {
}
