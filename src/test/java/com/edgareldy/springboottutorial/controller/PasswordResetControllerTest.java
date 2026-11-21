package com.edgareldy.springboottutorial.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.config.SecurityConfig;
import com.edgareldy.springboottutorial.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.ResetPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.VerifyOtpRequest;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.security.JwtAccessDeniedHandler;
import com.edgareldy.springboottutorial.security.JwtAuthenticationEntryPoint;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc tests for {@link PasswordResetController}. All four endpoints are
 * public (no authentication required), so no security context is needed. The
 * full filter chain is loaded via {@link SecurityConfig} to verify that the
 * permit-all rules cover each path.
 * <p>
 * Created edgar.muhamyangabo on 11/21/26
 * Author : edgar.muhamyangabo
 * Date : 11/21/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    @Test
    void _01_ShouldReturn200_WhenForgotPasswordWebCalled() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).forgotPassword("ada@example.com");
    }

    @Test
    void _02_ShouldReturn200_WhenForgotPasswordMobileCalled() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).forgotPasswordMobile("ada@example.com");
    }

    @Test
    void _03_ShouldReturn200_WhenVerifyOtpValid() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest("ada@example.com", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP is valid"));

        verify(userService).verifyPasswordResetOtp("ada@example.com", "123456");
    }

    @Test
    void _04_ShouldReturn400_WhenVerifyOtpInvalid() throws Exception {
        doThrow(new InvalidTokenException("Invalid or expired OTP"))
                .when(userService).verifyPasswordResetOtp("ada@example.com", "000000");

        mockMvc.perform(post("/api/v1/password-reset/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest("ada@example.com", "000000"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void _05_ShouldReturn200_WhenConfirmResetValid() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset/confirm")
                        .param("token", "reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("Password1@", "Password1@"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(userService).resetPassword("reset-otp", "Password1@");
    }

    @Test
    void _06_ShouldReturn400_WhenConfirmTokenInvalid() throws Exception {
        doThrow(new InvalidTokenException("Invalid or expired token"))
                .when(userService).resetPassword("bad-token", "Password1@");

        mockMvc.perform(post("/api/v1/password-reset/confirm")
                        .param("token", "bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("Password1@", "Password1@"))))
                .andExpect(status().isBadRequest());
    }
}
