package com.edgareldy.springboottutorial.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST /api/v1/auth/login}.
 * <p>
 * Clients identify themselves with their email address, not their username.
 * The Angular front-end was built against SST which used email as the login
 * identifier, so SBT must match that contract.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record LoginRequest(

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        String password
) {

    // Overrides the compiler-generated toString(): LoggingAspect logs every
    // service method argument via toString(), and login() takes this record
    // directly, so the default record toString() would put the raw password
    // in the application logs.
    @Override
    public String toString() {
        return "LoginRequest[email=%s, password=***]".formatted(email);
    }
}
