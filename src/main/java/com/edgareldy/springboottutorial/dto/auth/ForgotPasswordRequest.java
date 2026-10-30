package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the POST /auth/forgot-password endpoint. The provided
 * email address is used to look up the account and send a password-reset link.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid address")
        @JsonProperty("email") String email
) {
}
