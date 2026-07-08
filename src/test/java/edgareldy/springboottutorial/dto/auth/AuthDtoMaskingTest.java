package edgareldy.springboottutorial.dto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests locking in the masked {@code toString()} overrides on
 * {@link RegisterRequest}, {@link LoginRequest}, and {@link AuthResponse},
 * checked here since {@code LoggingAspect} logs every service method's
 * arguments and return value through them, and a regression back to the
 * compiler-generated {@code toString()} would leak a raw password or JWT.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
class AuthDtoMaskingTest {

    @Test
    void registerRequestToStringMasksPassword() {
        String result = new RegisterRequest("ada", "ada@example.com", "s3cr3tPassword").toString();

        assertThat(result).contains("ada", "ada@example.com").doesNotContain("s3cr3tPassword");
    }

    @Test
    void loginRequestToStringMasksPassword() {
        String result = new LoginRequest("ada", "s3cr3tPassword").toString();

        assertThat(result).contains("ada").doesNotContain("s3cr3tPassword");
    }

    @Test
    void authResponseToStringMasksToken() {
        String result = new AuthResponse("signed.jwt.token", "Bearer", "ada").toString();

        assertThat(result).contains("Bearer", "ada").doesNotContain("signed.jwt.token");
    }
}
