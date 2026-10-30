package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the POST /auth/reset-password endpoint. Carries the
 * one-time reset token (delivered by email) and the new plain-text password
 * to set after the token is validated and consumed.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Token must not be blank")
        @JsonProperty("token") String token,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "New password must be at least 8 characters")
        @JsonProperty("new_password") String newPassword
) {
}
