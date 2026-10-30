package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit tests for {@link CustomPermissionEvaluator}, verifying that
 * {@code hasPermission(resource, action)} matches the
 * {@code PERMISSION_<RESOURCE>:<ACTION>} authority format exactly.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
class CustomPermissionEvaluatorTest {

    private CustomPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CustomPermissionEvaluator();
    }

    private Authentication authWith(String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken("user", null, grantedAuthorities);
    }

    @Test
    void _01_ShouldGrantAccess_WhenAuthorityMatches() {
        Authentication auth = authWith("PERMISSION_PRODUCT:WRITE");

        assertThat(evaluator.hasPermission(auth, "PRODUCT", "WRITE")).isTrue();
    }

    @Test
    void _02_ShouldGrantAccess_WhenInputIsMixedCase() {
        Authentication auth = authWith("PERMISSION_PRODUCT:WRITE");

        assertThat(evaluator.hasPermission(auth, "product", "write")).isTrue();
    }

    @Test
    void _03_ShouldDenyAccess_WhenAuthorityMissing() {
        Authentication auth = authWith("PERMISSION_PRODUCT:READ");

        assertThat(evaluator.hasPermission(auth, "PRODUCT", "WRITE")).isFalse();
    }

    @Test
    void _04_ShouldDenyAccess_WhenAuthenticationIsNull() {
        assertThat(evaluator.hasPermission(null, "PRODUCT", "WRITE")).isFalse();
    }

    @Test
    void _05_ShouldDenyAccess_WhenResourceIsNull() {
        Authentication auth = authWith("PERMISSION_PRODUCT:WRITE");

        assertThat(evaluator.hasPermission(auth, null, "WRITE")).isFalse();
    }

    @Test
    void _06_ShouldDenyAccess_WhenPermissionIsNull() {
        Authentication auth = authWith("PERMISSION_PRODUCT:WRITE");

        assertThat(evaluator.hasPermission(auth, "PRODUCT", null)).isFalse();
    }

    @Test
    void _07_ShouldGrantAccess_ViaTargetIdOverload_WhenAuthorityMatches() {
        Authentication auth = authWith("PERMISSION_ORDER:READ");

        assertThat(evaluator.hasPermission(auth, 42L, "ORDER", "READ")).isTrue();
    }

    @Test
    void _08_ShouldDenyAccess_ViaTargetIdOverload_WhenAuthorityMissing() {
        Authentication auth = authWith("PERMISSION_ORDER:READ");

        assertThat(evaluator.hasPermission(auth, 42L, "ORDER", "DELETE")).isFalse();
    }
}
