package com.edgareldy.springboottutorial.controller;

import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.ResetPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import com.edgareldy.springboottutorial.service.AuthService;
import com.edgareldy.springboottutorial.service.UserService;
import jakarta.validation.Valid;
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
 * REST controller exposing registration, login, logout, and the current
 * user's profile. Delegates every operation to {@link AuthService}; no
 * business logic (password hashing, token generation) lives here.
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

    @GetMapping("/activate")
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
