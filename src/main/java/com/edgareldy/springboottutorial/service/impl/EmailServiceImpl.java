package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Default {@link EmailService} implementation using {@link JavaMailSender}.
 * Each method is annotated {@code @Async("emailTaskExecutor")} so the SMTP
 * call runs on the dedicated pool declared in {@code AsyncConfig}, never on
 * the HTTP request thread.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String baseUrl;

    public EmailServiceImpl(JavaMailSender mailSender, @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendActivationEmail(AppUser user, String token) {
        String link = baseUrl + "/api/v1/auth/activate?token=" + token;
        send(user.getEmail(), "Activate your account",
                "Hello " + user.getUsername() + ",\n\nActivate your account by visiting:\n" + link);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(AppUser user, String token) {
        String link = baseUrl + "/api/v1/auth/reset-password?token=" + token;
        send(user.getEmail(), "Reset your password",
                "Hello " + user.getUsername() + ",\n\nReset your password by visiting:\n" + link);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Sent email to {} with subject '{}'", to, subject);
    }
}
