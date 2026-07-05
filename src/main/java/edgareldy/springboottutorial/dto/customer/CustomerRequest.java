package edgareldy.springboottutorial.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/customers}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CustomerRequest(

        @NotBlank(message = "firstName must not be blank")
        String firstName,

        @NotBlank(message = "lastName must not be blank")
        String lastName,

        @NotBlank(message = "telephone must not be blank")
        @Pattern(regexp = "^\\+?[0-9()\\-\\s]{7,20}$", message = "telephone must be a valid phone number")
        String telephone,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "address must not be blank")
        String address
) {
}
