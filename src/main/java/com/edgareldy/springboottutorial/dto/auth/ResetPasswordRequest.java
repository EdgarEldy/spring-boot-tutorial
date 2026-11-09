package com.edgareldy.springboottutorial.dto.auth;

import com.edgareldy.springboottutorial.validation.password.PasswordsMatch;
import com.edgareldy.springboottutorial.validation.password.ValidPassword;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the POST /auth/reset-password endpoint. Carries the new
 * password and its confirmation; the one-time reset token is passed as a
 * query parameter so the frontend can forward it directly from the email
 * link without re-embedding it in a JSON payload.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@PasswordsMatch(passwordField = "newPassword", confirmPasswordField = "confirmPassword",
        message = "Passwords must match")
public record ResetPasswordRequest(
        @NotBlank(message = "New password must not be blank")
        @ValidPassword
        @JsonProperty("new_password") String newPassword,

        @NotBlank(message = "Password confirmation must not be blank")
        @JsonProperty("confirm_password") String confirmPassword
) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[newPassword=***, confirmPassword=***]";
    }
}
