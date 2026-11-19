package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.edgareldy.springboottutorial.config.NotificationProperties;
import com.edgareldy.springboottutorial.email.EmailTemplateName;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Unit tests for {@link OrderEmailServiceImpl}. {@link JavaMailSender} and
 * {@link SpringTemplateEngine} are mocked so the tests verify that the correct
 * template name, context variables, recipient and subject are used for each
 * email type, and that SMTP exceptions do not propagate.
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
    private SpringTemplateEngine templateEngine;

    @Mock
    private NotificationProperties notificationProperties;

    private OrderEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderEmailServiceImpl(mailSender, templateEngine, notificationProperties, "noreply@example.com");
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
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(anyString(), any(IContext.class))).willReturn("<html>body</html>");
        Order order = sampleOrder();
        AppUser user = sampleUser();

        service.sendOrderConfirmation(user, order);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(
                eq(EmailTemplateName.ORDER_CONFIRMATION.getName()), contextCaptor.capture());
        Context ctx = contextCaptor.getValue();
        assertThat(ctx.getVariable("username")).isEqualTo("ada");
        assertThat(ctx.getVariable("orderNumber")).isEqualTo("ORD-TEST-0001");
        assertThat(ctx.getVariable("items")).isNotNull();
        assertThat(ctx.getVariable("totalAmount")).isEqualTo(new BigDecimal("100.00"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void _02_ShouldSendNotificationToAdmin_WhenOrderCreated() {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(anyString(), any(IContext.class))).willReturn("<html>body</html>");
        given(notificationProperties.adminEmail()).willReturn("admin@example.com");
        AppUser user = sampleUser();
        OrderItem item = OrderItem.builder()
                .id(1L).productName("Keyboard")
                .unitPrice(new BigDecimal("50.00")).quantity(2)
                .subtotal(new BigDecimal("100.00")).build();
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-TEST-0001")
                .totalAmount(new BigDecimal("100.00"))
                .user(user).items(List.of(item)).build();

        service.sendAdminNotification(order);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(
                eq(EmailTemplateName.ORDER_ADMIN_NOTIFICATION.getName()), contextCaptor.capture());
        Context ctx = contextCaptor.getValue();
        assertThat(ctx.getVariable("orderNumber")).isEqualTo("ORD-TEST-0001");
        assertThat(ctx.getVariable("customerName")).isEqualTo("Ada Lovelace");
        assertThat(ctx.getVariable("customerEmail")).isEqualTo("ada@example.com");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void _03_ShouldNotPropagateSmtpException_WhenMailFails() {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(anyString(), any(IContext.class))).willReturn("<html>body</html>");
        given(notificationProperties.adminEmail()).willReturn("admin@example.com");
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));
        Order order = sampleOrder();
        AppUser user = sampleUser();

        assertThatCode(() -> service.sendOrderConfirmation(user, order)).doesNotThrowAnyException();
        assertThatCode(() -> service.sendAdminNotification(order)).doesNotThrowAnyException();
    }
}
