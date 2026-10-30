package com.edgareldy.springboottutorial.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request body for an admin updating a user's username or email. Both fields
 * are optional: omitting one leaves it unchanged. When supplied, each field
 * must satisfy its own uniqueness constraint, checked in
 * {@code UserServiceImpl} before the update is persisted.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
        @Email(message = "Email must be a valid email address") String email
) {
}
