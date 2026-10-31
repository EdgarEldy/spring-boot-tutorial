package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.config.SecurityConfig;
import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.ResetPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.security.JwtAccessDeniedHandler;
import com.edgareldy.springboottutorial.security.JwtAuthenticationEntryPoint;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.AuthService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link AuthController}, with
 * {@link AuthService} mocked. Unlike every other controller test in this
 * project, security filters are left enabled ({@code SecurityConfig} is
 * explicitly imported): register/login must be reachable without
 * authentication and {@code /me} needs a real {@code SecurityContext} to
 * resolve its {@code @AuthenticationPrincipal}, none of which the disabled
 * filter chain used elsewhere would exercise.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    // Unused directly: JwtAuthFilter/SecurityConfig need these to construct,
    // even though no request in this class carries a JWT.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // JwtAuthFilter also needs BlacklistedTokenService to construct.
    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    @Test
    void _01_ShouldReturn201_WhenRegisterRequestValid() throws Exception {
        UserResponse response = new UserResponse(1L, "ada", "ada@example.com", false, false,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void _02_ShouldReturn400_WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _03_ShouldReturn422_WhenUsernameAlreadyUsed() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessRuleException("Username ada is already in use"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "password123"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void _04_ShouldReturn200_WhenCredentialsValid() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("signed-token", "Bearer", "ada"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ada@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("signed-token"))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"));
    }

    @Test
    void _05_ShouldReturn401_WhenCredentialsInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ada@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void _08_ShouldReturn400_WhenLoginEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-an-email", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _06_ShouldReturnCallerProfile_WhenAuthenticated() throws Exception {
        when(authService.me("ada")).thenReturn(new UserResponse(1L, "ada", "ada@example.com", true, false,
                List.of(new RoleResponse(1L, "USER", List.of()))));

        mockMvc.perform(get("/api/v1/auth/me").with(user("ada").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void _07_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void _08_ShouldReturn200_WhenActivationTokenValid() throws Exception {
        UserResponse activated = new UserResponse(1L, "ada", "ada@example.com", true, false,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(userService.activate("valid-token")).thenReturn(activated);

        mockMvc.perform(get("/api/v1/auth/activate").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void _09_ShouldReturn400_WhenActivationTokenInvalid() throws Exception {
        when(userService.activate("bad-token"))
                .thenThrow(new InvalidTokenException("Invalid activation token"));

        mockMvc.perform(get("/api/v1/auth/activate").param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void _10_ShouldReturn200_WhenForgotPasswordRequestValid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _11_ShouldReturn400_WhenForgotPasswordEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void _12_ShouldReturn200_WhenResetPasswordRequestValid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("valid-token", "newpassword"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _13_ShouldReturn400_WhenResetPasswordTokenInvalid() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidTokenException("Invalid password reset token"))
                .when(userService).resetPassword("bad-token", "newpassword");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("bad-token", "newpassword"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void _14_ShouldReturn200_WhenLogoutWithValidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer signed-token")
                        .with(user("ada").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("signed-token");
    }

    @Test
    void _15_ShouldReturn401_WhenLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isUnauthorized());
    }
}
