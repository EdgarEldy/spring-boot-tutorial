package edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edgareldy.springboottutorial.config.SecurityConfig;
import edgareldy.springboottutorial.dto.auth.AuthResponse;
import edgareldy.springboottutorial.dto.auth.LoginRequest;
import edgareldy.springboottutorial.dto.auth.RegisterRequest;
import edgareldy.springboottutorial.dto.auth.UserResponse;
import edgareldy.springboottutorial.entity.user.Role;
import edgareldy.springboottutorial.exception.BusinessRuleException;
import edgareldy.springboottutorial.security.JwtAccessDeniedHandler;
import edgareldy.springboottutorial.security.JwtAuthenticationEntryPoint;
import edgareldy.springboottutorial.security.JwtService;
import edgareldy.springboottutorial.service.AuthService;
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

    // Unused directly: JwtAuthFilter/SecurityConfig need these to construct,
    // even though no request in this class carries a JWT.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void registerReturns201WhenValid() throws Exception {
        UserResponse response = new UserResponse(1L, "ada", "ada@example.com", Role.USER);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void registerReturns400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registerReturns422WhenUsernameAlreadyUsed() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessRuleException("Username ada is already in use"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ada", "ada@example.com", "password123"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void loginReturns200WhenCredentialsAreValid() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("signed-token", "Bearer", "ada"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ada", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("signed-token"));
    }

    @Test
    void loginReturns401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ada", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCallerProfile() throws Exception {
        when(authService.me("ada")).thenReturn(new UserResponse(1L, "ada", "ada@example.com", Role.USER));

        mockMvc.perform(get("/api/v1/auth/me").with(user("ada").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void meReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
