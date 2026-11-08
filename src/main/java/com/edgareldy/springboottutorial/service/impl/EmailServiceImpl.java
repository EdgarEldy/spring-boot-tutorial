package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.EmailTemplateName;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Default {@link EmailService} implementation using {@link JavaMailSender} and
 * {@link SpringTemplateEngine} to render HTML email bodies from Thymeleaf
 * templates located in {@code src/main/resources/templates/}. Template names
 * are resolved via {@link EmailTemplateName} to avoid hardcoded strings in
 * the service methods. Each method is annotated
 * {@code @Async("emailTaskExecutor")} so the SMTP call runs on the dedicated
 * pool declared in {@code AsyncConfig}, never on the HTTP request thread.
 * Email links target the Angular frontend ({@code app.frontend-url}) so the
 * user lands on the correct UI page before the token is submitted to the API.
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
    private final SpringTemplateEngine templateEngine;
    private final String frontendUrl;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendActivationEmail(AppUser user, String token) {
        String link = frontendUrl + "/auth/activate?token=" + token;
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getUsername());
        context.setVariable("activationToken", token);
        context.setVariable("activationLink", link);
        String html = templateEngine.process(EmailTemplateName.ACTIVATE_ACCOUNT.getName(), context);
        sendEmail(user.getEmail(), "Activate your account", html);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(AppUser user, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getUsername());
        context.setVariable("resetLink", link);
        String html = templateEngine.process(EmailTemplateName.RESET_PASSWORD.getName(), context);
        sendEmail(user.getEmail(), "Reset your password", html);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MULTIPART_MODE_MIXED,
                    UTF_8.name()
            );
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Sent email to {} with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} - {}", to, e.getMessage());
        }
    }
}
