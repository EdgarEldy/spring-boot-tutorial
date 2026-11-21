package com.edgareldy.springboottutorial.controller;

import com.edgareldy.springboottutorial.dto.auth.ForgotPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.ResetPasswordRequest;
import com.edgareldy.springboottutorial.dto.auth.VerifyOtpRequest;
import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import com.edgareldy.springboottutorial.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the two password-reset flows: web (UUID link, 1-hour TTL)
 * and mobile (6-digit OTP, 5-minute TTL). The mobile flow has a dedicated
 * verify-otp step so the client can confirm the code before displaying the
 * new-password screen; the same OTP is then submitted to /confirm.
 * <p>
 * Created edgar.muhamyangabo on 11/21/26
 * Author : edgar.muhamyangabo
 * Date : 11/21/26
 * Project : spring-boot-tutorial
 */
@RestController
@RequestMapping("/api/v1/password-reset")
@RequiredArgsConstructor
@Tag(name = "Password Reset")
public class PasswordResetController {

    private final UserService userService;

    @Operation(summary = "Request a password-reset link (web flow)")
    @PostMapping("/web")
    public ResponseEntity<ApiResponse<Void>> forgotPasswordWeb(
            @Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.email());
        return ResponseEntity.ok(
                ApiResponse.success(null, "If the email is registered a reset link has been sent"));
    }

    @Operation(summary = "Request a password-reset OTP (mobile flow)")
    @PostMapping("/mobile")
    public ResponseEntity<ApiResponse<Void>> forgotPasswordMobile(
            @Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPasswordMobile(request.email());
        return ResponseEntity.ok(
                ApiResponse.success(null, "If the email is registered a reset code has been sent"));
    }

    @Operation(summary = "Verify a password-reset OTP without consuming it")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        userService.verifyPasswordResetOtp(request.email(), request.otp());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP is valid"));
    }

    @Operation(summary = "Confirm password reset with a valid token or OTP")
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(token, request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
