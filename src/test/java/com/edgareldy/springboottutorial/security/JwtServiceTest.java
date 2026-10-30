package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JwtService}. No Spring context: the class only
 * depends on the {@link JwtProperties} instance normally bound from
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
        jwtService = new JwtService(new JwtProperties(SECRET, 3600000L));
    }

    @Test
    void _01_ShouldEncodeUsernameAsSubject_WhenGeneratingToken() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
    }

    @Test
    void _02_ShouldEmbedUniqueJti_WhenGeneratingTwoTokens() {
        String token1 = jwtService.generateToken("ada");
        String token2 = jwtService.generateToken("ada");

        String jti1 = jwtService.extractJti(token1);
        String jti2 = jwtService.extractJti(token2);

        assertThat(jti1).isNotNull().isNotEmpty();
        assertThat(jti2).isNotNull().isNotEmpty();
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void _03_ShouldReturnFutureExpiration_WhenTokenIsValid() {
        String token = jwtService.generateToken("ada");

        Instant expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(Instant.now());
    }

    @Test
    void _04_ShouldThrowJwtException_WhenTokenTampered() {
        String token = jwtService.generateToken("ada");

        assertThatThrownBy(() -> jwtService.extractUsername(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }
}
