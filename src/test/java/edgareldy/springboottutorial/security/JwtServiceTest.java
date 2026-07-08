package edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JwtService}. No Spring context: the class only
 * depends on the two constructor values normally injected from
 * {@code jwt.secret}/{@code jwt.expiration-ms}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-at-least-32-bytes-long-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000L);
    }

    @Test
    void generateTokenEncodesUsernameAsSubject() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
    }

    @Test
    void isTokenValidReturnsTrueForMatchingUsername() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token, "ada")).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUsername() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token, "grace")).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(SECRET, 1L);
        String token = shortLivedService.generateToken("ada");
        Thread.sleep(10);

        assertThat(shortLivedService.isTokenValid(token, "ada")).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForTamperedToken() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token + "tampered", "ada")).isFalse();
    }
}
