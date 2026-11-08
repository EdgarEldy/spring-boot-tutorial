package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.email.EmailTemplateName;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Unit tests for {@link EmailServiceImpl}, with {@link JavaMailSender} and
 * {@link SpringTemplateEngine} mocked. Verifies that the correct template name
 * (via {@link EmailTemplateName}), context variables, and frontend URLs are
 * used for each email type, and that the resulting HTML is dispatched via
 * {@link JavaMailSender}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    private static final String FRONTEND_URL = "http://localhost:4200";
    private static final String FROM_ADDRESS = "noreply@spring-boot-tutorial.com";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private EmailServiceImpl emailService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, templateEngine, FRONTEND_URL, FROM_ADDRESS);
        user = AppUser.builder().id(1L).username("ada").email("ada@example.com").build();
    }

    @Test
    void _01_ShouldPassFrontendActivationLink_WhenActivationEmailIsSent() {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(anyString(), any(IContext.class))).willReturn("<html>test</html>");
        emailService.sendActivationEmail(user, "raw-token");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(
                eq(EmailTemplateName.ACTIVATE_ACCOUNT.getName()), contextCaptor.capture());
        Context ctx = contextCaptor.getValue();
        assertThat(ctx.getVariable("username")).isEqualTo("ada");
        assertThat(ctx.getVariable("activationLink"))
                .isEqualTo(FRONTEND_URL + "/auth/activate?token=raw-token");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void _02_ShouldPassFrontendResetLink_WhenPasswordResetEmailIsSent() {
        given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
        given(templateEngine.process(anyString(), any(IContext.class))).willReturn("<html>test</html>");
        emailService.sendPasswordResetEmail(user, "reset-token");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(
                eq(EmailTemplateName.RESET_PASSWORD.getName()), contextCaptor.capture());
        Context ctx = contextCaptor.getValue();
        assertThat(ctx.getVariable("username")).isEqualTo("ada");
        assertThat(ctx.getVariable("resetLink"))
                .isEqualTo(FRONTEND_URL + "/auth/reset-password?token=reset-token");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void _03_ShouldRunOnEmailTaskExecutor_WhenEmailsAreSent() throws NoSuchMethodException {
        var sendActivation = EmailServiceImpl.class.getMethod(
                "sendActivationEmail", AppUser.class, String.class);
        var sendReset = EmailServiceImpl.class.getMethod(
                "sendPasswordResetEmail", AppUser.class, String.class);

        assertThat(sendActivation.getAnnotation(
                org.springframework.scheduling.annotation.Async.class).value())
                .isEqualTo("emailTaskExecutor");
        assertThat(sendReset.getAnnotation(
                org.springframework.scheduling.annotation.Async.class).value())
                .isEqualTo("emailTaskExecutor");
    }
}
