package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.config.NotificationProperties;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.service.OrderEmailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Default {@link OrderEmailService} implementation using {@link JavaMailSender}
 * to send plain-text order notifications. Each method is annotated
 * {@code @Async("emailTaskExecutor")} so the SMTP call runs on the dedicated
 * pool declared in {@code AsyncConfig}. SMTP failures are caught and logged
 * at ERROR level; they are never re-thrown because the order has already
 * committed and an email failure must not make the checkout appear broken.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@Service
public class OrderEmailServiceImpl implements OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;
    private final String fromAddress;

    public OrderEmailServiceImpl(JavaMailSender mailSender,
                                 NotificationProperties notificationProperties,
                                 @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.notificationProperties = notificationProperties;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOrderConfirmation(AppUser user, Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Order Confirmation - " + order.getOrderNumber());
            message.setText(buildConfirmationBody(user, order));
            mailSender.send(message);
            log.info("Order confirmation sent to {} for order {}", user.getEmail(), order.getOrderNumber());
        } catch (MailException e) {
            log.error("Failed to send order confirmation to {} for order {} - {}",
                    user.getEmail(), order.getOrderNumber(), e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendAdminNotification(Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(notificationProperties.adminEmail());
            message.setSubject("New Order Received - " + order.getOrderNumber());
            message.setText(buildAdminBody(order));
            mailSender.send(message);
            log.info("Admin notification sent for order {}", order.getOrderNumber());
        } catch (MailException e) {
            log.error("Failed to send admin notification for order {} - {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    private String buildConfirmationBody(AppUser user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order: ").append(order.getOrderNumber()).append("\n");
        sb.append("Date: ").append(LocalDateTime.now().format(DATE_FMT)).append("\n\n");
        sb.append("Items:\n");
        for (OrderItem item : order.getItems()) {
            sb.append("- ").append(item.getProductName())
                    .append(" x").append(item.getQuantity())
                    .append(" @ ").append(item.getUnitPrice())
                    .append(" = ").append(item.getSubtotal()).append("\n");
        }
        sb.append("\nTotal: ").append(order.getTotalAmount()).append("\n\n");
        sb.append("Thank you for your order.");
        return sb.toString();
    }

    private String buildAdminBody(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("New order received: ").append(order.getOrderNumber()).append("\n");
        if (order.getUser() != null) {
            AppUser user = order.getUser();
            String name = (user.getFirstName() != null ? user.getFirstName() : "")
                    + " " + (user.getLastName() != null ? user.getLastName() : "");
            sb.append("Customer: ").append(name.trim())
                    .append(" (").append(user.getEmail()).append(")\n");
        }
        sb.append("Date: ").append(LocalDateTime.now().format(DATE_FMT)).append("\n\n");
        sb.append("Items:\n");
        for (OrderItem item : order.getItems()) {
            sb.append("- ").append(item.getProductName())
                    .append(" x").append(item.getQuantity())
                    .append(" @ ").append(item.getUnitPrice())
                    .append(" = ").append(item.getSubtotal()).append("\n");
        }
        sb.append("\nTotal: ").append(order.getTotalAmount());
        return sb.toString();
    }
}
