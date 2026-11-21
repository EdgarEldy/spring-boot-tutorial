package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for the account activation endpoint. Carries the email
 * address and the 6-digit OTP the user received by email. Both fields
 * are required; the OTP must consist of exactly 6 digits.
 * <p>
 * Created edgar.muhamyangabo on 11/21/26
 * Author : edgar.muhamyangabo
 * Date : 11/21/26
 * Project : spring-boot-tutorial
 */
public record ActivateAccountRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Activation code must not be blank")
        @Pattern(regexp = "\\d{6}", message = "Activation code must be exactly 6 digits")
        @JsonProperty("activation_code")
        String otp
) {
}
