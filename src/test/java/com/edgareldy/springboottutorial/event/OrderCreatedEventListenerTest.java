package com.edgareldy.springboottutorial.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.service.OrderEmailService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link OrderCreatedEventListener}. The {@code @Async} and
 * {@code @TransactionalEventListener} annotations are both bypassed in a
 * pure Mockito context, so {@code onOrderCreated} is called directly and we
 * verify the email service interactions.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEmailService orderEmailService;

    @InjectMocks
    private OrderCreatedEventListener listener;

    private AppUser sampleUser() {
        return AppUser.builder()
                .id(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .username("ada")
                .email("ada@example.com")
                .password("hashed")
                .build();
    }

    private Order orderWithUser(AppUser user) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productName("Keyboard")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(2)
                .subtotal(new BigDecimal("100.00"))
                .build();
        return Order.builder()
                .id(1L)
                .orderNumber("ORD-TEST-0001")
                .totalAmount(new BigDecimal("100.00"))
                .user(user)
                .items(List.of(item))
                .build();
    }

    @Test
    void _01_ShouldCallBothEmailMethods_WhenOrderHasUser() {
        AppUser user = sampleUser();
        Order order = orderWithUser(user);
        OrderCreatedEvent event = new OrderCreatedEvent(1L, null, null, 2, new BigDecimal("100.00"), "ORD-TEST-0001");
        when(orderRepository.findByIdWithItemsAndUser(1L)).thenReturn(Optional.of(order));

        listener.onOrderCreated(event);

        verify(orderEmailService).sendOrderConfirmation(user, order);
        verify(orderEmailService).sendAdminNotification(order);
    }

    @Test
    void _02_ShouldSkipConfirmation_WhenOrderHasNoUser() {
        Order order = orderWithUser(null);
        OrderCreatedEvent event = new OrderCreatedEvent(1L, null, null, 2, new BigDecimal("100.00"), "ORD-TEST-0001");
        when(orderRepository.findByIdWithItemsAndUser(1L)).thenReturn(Optional.of(order));

        listener.onOrderCreated(event);

        verify(orderEmailService, never()).sendOrderConfirmation(null, order);
        verify(orderEmailService).sendAdminNotification(order);
    }

    @Test
    void _03_ShouldThrowResourceNotFoundException_WhenOrderNotFoundAfterCommit() {
        OrderCreatedEvent event = new OrderCreatedEvent(99L, null, null, 1, new BigDecimal("10.00"), "ORD-MISSING");
        when(orderRepository.findByIdWithItemsAndUser(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onOrderCreated(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
