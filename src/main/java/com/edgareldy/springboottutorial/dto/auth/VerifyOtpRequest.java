package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the OTP verification step of the mobile password-reset flow.
 * The client calls POST /api/v1/password-reset/verify-otp to confirm the OTP is
 * valid before showing the new-password screen. The same OTP is then reused in
 * the /confirm call: since each user has at most one pending reset token at a
 * time, the non-destructive check here does not invalidate the token.
 * <p>
 * Created edgar.muhamyangabo on 11/21/26
 * Author : edgar.muhamyangabo
 * Date : 11/21/26
 * Project : spring-boot-tutorial
 */
public record VerifyOtpRequest(

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        @JsonProperty("email")
        String email,

        @NotBlank(message = "OTP must not be blank")
        @JsonProperty("otp")
        String otp
) {}
