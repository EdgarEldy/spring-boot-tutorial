package com.edgareldy.springboottutorial.controller;

import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.ResetPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.AuthService;
import com.edgareldy.springboottutorial.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing registration, login, logout, the current user
 * profile, account activation, and password-reset flows. Delegates every
 * operation to {@link AuthService} (credentials) or {@link UserService}
 * (token flows); no business logic lives here.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        String rawToken = authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length())
                : authHeader;
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(rawToken), "Token refreshed successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request), "Account created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String rawToken = authHeader.substring(BEARER_PREFIX.length());
        authService.logout(rawToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(userDetails.getUsername()), "Profile retrieved successfully"));
    }

    @GetMapping("/token-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> tokenInfo(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length())
                : authHeader;
        Map<String, Object> claims = jwtService.extractAllClaims(token);
        return ResponseEntity.ok(ApiResponse.success(claims, "Token claims retrieved successfully"));
    }

    @GetMapping("/activate-account")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(userService.activate(token), "Account activated successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "If the email is registered a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(token, request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
