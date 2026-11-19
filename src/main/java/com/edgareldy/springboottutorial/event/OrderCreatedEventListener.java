package com.edgareldy.springboottutorial.event;

import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.service.OrderEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumes {@link OrderCreatedEvent} to send post-commit email notifications.
 * Uses {@code @TransactionalEventListener(AFTER_COMMIT)} so the event only
 * fires after the order row is durable - a rollback will not trigger emails.
 * Combined with {@code @Async}, the notification work runs on the
 * {@code emailTaskExecutor} pool, so SMTP latency never blocks the original
 * request thread or the Spring event-publishing thread.
 * <p>
 * The order is re-loaded from the database (with items and user eagerly
 * fetched) rather than relying on the event payload, because the original
 * transaction has already closed by the time this method runs and passing
 * managed entities across transaction boundaries is unreliable.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderEmailService orderEmailService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order {} created, sending notifications", event.orderNumber());
        Order order = orderRepository.findByIdWithItemsAndUser(event.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found after commit: " + event.orderId()));
        AppUser user = order.getUser();
        if (user != null) {
            orderEmailService.sendOrderConfirmation(user, order);
        }
        orderEmailService.sendAdminNotification(order);
    }
}
