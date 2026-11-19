package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.config.NotificationProperties;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for {@link OrderEmailServiceImpl}. The {@code @Async} annotation
 * is ignored in a pure Mockito context (no Spring proxy), so methods are
 * invoked synchronously here and we simply verify that
 * {@link JavaMailSender#send(SimpleMailMessage)} is called with the expected
 * recipient and subject, and that SMTP exceptions do not propagate.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderEmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationProperties notificationProperties;

    private OrderEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        when(notificationProperties.adminEmail()).thenReturn("admin@example.com");
        service = new OrderEmailServiceImpl(mailSender, notificationProperties, "noreply@example.com");
    }

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

    private Order sampleOrder() {
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
                .items(List.of(item))
                .build();
    }

    @Test
    void _01_ShouldSendConfirmationToUser_WhenOrderCreated() {
        Order order = sampleOrder();
        AppUser user = sampleUser();

        service.sendOrderConfirmation(user, order);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assert sent.getTo() != null;
        assert sent.getTo()[0].equals("ada@example.com");
        assert sent.getSubject() != null && sent.getSubject().contains("ORD-TEST-0001");
    }

    @Test
    void _02_ShouldSendNotificationToAdmin_WhenOrderCreated() {
        Order order = sampleOrder();

        service.sendAdminNotification(order);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assert sent.getTo() != null;
        assert sent.getTo()[0].equals("admin@example.com");
        assert sent.getSubject() != null && sent.getSubject().contains("ORD-TEST-0001");
    }

    @Test
    void _03_ShouldNotPropagateSmtpException_WhenMailFails() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        Order order = sampleOrder();
        AppUser user = sampleUser();

        assertThatCode(() -> service.sendOrderConfirmation(user, order))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.sendAdminNotification(order))
                .doesNotThrowAnyException();
    }
}
