package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for sending order-related transactional emails. Implementations
 * run asynchronously on the {@code emailTaskExecutor} pool so a slow SMTP
 * call never blocks the event-listener thread that triggered it. SMTP
 * failures are logged at ERROR level and never propagated - a failed email
 * must never cause the order transaction (already committed) to appear
 * failed.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public interface OrderEmailService {

    /**
     * Sends an order confirmation to the customer who placed the order.
     *
     * @param user  the authenticated user who placed the order
     * @param order the newly created order with its items loaded
     */
    void sendOrderConfirmation(AppUser user, Order order);

    /**
     * Sends a new-order alert to the configured admin address.
     *
     * @param order the newly created order with its items loaded
     */
    void sendAdminNotification(Order order);
}
