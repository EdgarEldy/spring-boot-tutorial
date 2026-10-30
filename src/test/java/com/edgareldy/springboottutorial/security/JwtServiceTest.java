package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.Set;
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
    private AppUser ada;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 3600000L));
        Permission readPermission = Permission.builder().id(1L).resource("PRODUCT").action("READ").build();
        Role userRole = Role.builder().id(1L).roleName("USER")
                .permissions(Set.of(readPermission)).build();
        ada = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com")
                .password("hashed").enabled(true).roles(Set.of(userRole)).build();
    }

    @Test
    void _01_ShouldEncodeUsernameAsSubject_WhenGeneratingToken() {
        String token = jwtService.generateToken(ada);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada");
    }

    @Test
    void _02_ShouldEmbedUniqueJti_WhenGeneratingTwoTokens() {
        String token1 = jwtService.generateToken(ada);
        String token2 = jwtService.generateToken(ada);

        String jti1 = jwtService.extractJti(token1);
        String jti2 = jwtService.extractJti(token2);

        assertThat(jti1).isNotNull().isNotEmpty();
        assertThat(jti2).isNotNull().isNotEmpty();
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void _03_ShouldReturnFutureExpiration_WhenTokenIsValid() {
        String token = jwtService.generateToken(ada);

        Instant expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(Instant.now());
    }

    @Test
    void _04_ShouldThrowJwtException_WhenTokenTampered() {
        String token = jwtService.generateToken(ada);

        assertThatThrownBy(() -> jwtService.extractUsername(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void _05_ShouldEmbedRolesAndPermissions_WhenUserHasRoles() {
        AppUser admin = AppUser.builder()
                .id(2L).username("admin").email("admin@example.com")
                .password("hashed").enabled(true)
                .roles(Set.of(
                        Role.builder().id(2L).roleName("ADMIN")
                                .permissions(Set.of(
                                        Permission.builder().id(2L).resource("PRODUCT").action("WRITE").build()))
                                .build()))
                .build();

        String token = jwtService.generateToken(admin);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractJti(token)).isNotBlank();
    }
}
