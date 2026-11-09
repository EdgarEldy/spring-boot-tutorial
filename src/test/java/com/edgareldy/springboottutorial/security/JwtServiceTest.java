package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;

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
    private static final long EXPIRATION_MS = 3600000L;
    private static final long REFRESH_EXPIRATION_MS = 604800000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS));
    }

    @Test
    void _01_ShouldEncodeUsernameAsSubject_WhenGeneratingToken() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
    }

    @Test
    void _02_ShouldReturnTrue_WhenUsernameMatches() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token, "ada")).isTrue();
    }

    @Test
    void _03_ShouldReturnFalse_WhenUsernameDiffers() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token, "grace")).isFalse();
    }

    @Test
    void _04_ShouldReturnFalse_WhenTokenExpired() throws InterruptedException {
        JwtService shortLivedService = new JwtService(new JwtProperties(SECRET, 1L, REFRESH_EXPIRATION_MS));
        String token = shortLivedService.generateToken("ada");
        Thread.sleep(10);

        assertThat(shortLivedService.isTokenValid(token, "ada")).isFalse();
    }

    @Test
    void _05_ShouldReturnFalse_WhenTokenTampered() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.isTokenValid(token + "tampered", "ada")).isFalse();
    }

    @Test
    void _06_ShouldEncodeSubjectInRefreshToken() {
        String token = jwtService.generateRefreshToken("ada");

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
    }

    @Test
    void _07_ShouldDetectRefreshToken_WhenTokenTypeIsRefresh() {
        String refreshToken = jwtService.generateRefreshToken("ada");
        String accessToken = jwtService.generateToken("ada");

        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    void _08_ShouldReturnAllClaims_WhenTokenValid() {
        String token = jwtService.generateToken("ada");

        assertThat(jwtService.extractAllClaims(token)).containsKey("sub");
        assertThat(jwtService.extractAllClaims(token).get("sub")).isEqualTo("ada");
    }
}
