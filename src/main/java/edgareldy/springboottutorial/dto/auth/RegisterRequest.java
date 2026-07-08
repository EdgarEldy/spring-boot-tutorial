package edgareldy.springboottutorial.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload accepted by {@code POST /api/v1/auth/register}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record RegisterRequest(

        @NotBlank(message = "username must not be blank")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {

    // Overrides the compiler-generated toString(): LoggingAspect logs every
    // service method argument via toString(), and register() takes this
    // record directly, so the default record toString() would put the raw
    // password in the application logs.
    @Override
    public String toString() {
        return "RegisterRequest[username=%s, email=%s, password=***]".formatted(username, email);
    }
}
