package com.edgareldy.springboottutorial.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/customers}.
 * The JSON contract is snake_case, decoupled from the Java fields' own
 * camelCase names via explicit {@link JsonProperty} annotations.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CustomerRequest(

        @JsonProperty("first_name")
        @NotBlank(message = "First name must not be blank")
        String firstName,

        @JsonProperty("last_name")
        @NotBlank(message = "Last name must not be blank")
        String lastName,

        @NotBlank(message = "Telephone must not be blank")
        @Pattern(regexp = "^\\+?[0-9()\\-\\s]{7,20}$", message = "Telephone must be a valid phone number")
        String telephone,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Address must not be blank")
        String address
) {
}
