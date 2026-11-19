package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.config.NotificationProperties;
import com.edgareldy.springboottutorial.email.EmailTemplateName;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.service.OrderEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

/**
 * Default {@link OrderEmailService} implementation using {@link JavaMailSender}
 * and {@link SpringTemplateEngine} to render HTML email bodies from Thymeleaf
 * templates. Each method is annotated {@code @Async("emailTaskExecutor")} so
 * the SMTP call runs on the dedicated pool declared in {@code AsyncConfig}.
 * SMTP and messaging failures are caught and logged at ERROR level; they are
 * never re-thrown because the order has already committed and an email failure
 * must not make the checkout appear broken.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@Service
public class OrderEmailServiceImpl implements OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationProperties notificationProperties;
    private final String fromAddress;

    public OrderEmailServiceImpl(JavaMailSender mailSender,
                                 SpringTemplateEngine templateEngine,
                                 NotificationProperties notificationProperties,
                                 @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.notificationProperties = notificationProperties;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOrderConfirmation(AppUser user, Order order) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getUsername());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("items", order.getItems());
        context.setVariable("totalAmount", order.getTotalAmount());
        String html = templateEngine.process(EmailTemplateName.ORDER_CONFIRMATION.getName(), context);
        sendEmail(user.getEmail(), "Order Confirmation - " + order.getOrderNumber(), html);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendAdminNotification(Order order) {
        String customerName = "N/A";
        String customerEmail = null;
        if (order.getUser() != null) {
            AppUser user = order.getUser();
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            String fullName = (first + " " + last).trim();
            customerName = fullName.isEmpty() ? user.getUsername() : fullName;
            customerEmail = user.getEmail();
        } else if (order.getCustomer() != null) {
            customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
            customerEmail = order.getCustomer().getEmail();
        }
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("customerName", customerName);
        context.setVariable("customerEmail", customerEmail);
        context.setVariable("items", order.getItems());
        context.setVariable("totalAmount", order.getTotalAmount());
        String html = templateEngine.process(EmailTemplateName.ORDER_ADMIN_NOTIFICATION.getName(), context);
        sendEmail(notificationProperties.adminEmail(), "New Order Received - " + order.getOrderNumber(), html);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MULTIPART_MODE_MIXED, UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Sent email to {} with subject '{}'", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to {} - {}", to, e.getMessage());
        }
    }
}
