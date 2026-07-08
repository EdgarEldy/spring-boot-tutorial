package edgareldy.springboottutorial.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST /api/v1/auth/login}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record LoginRequest(

        @NotBlank(message = "username must not be blank")
        String username,

        @NotBlank(message = "password must not be blank")
        String password
) {
}
