package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.token.BlacklistedToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.entity.user.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} for {@link BlacklistedTokenRepository}, backed by a
 * real PostgreSQL instance via Testcontainers.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestcontainersConfiguration.class)
class BlacklistedTokenRepositoryTest {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = appUserRepository.save(AppUser.builder()
                .username("ada").email("ada@example.com")
                .password("hashed").role(Role.USER).build());
    }

    @Test
    void _01_ShouldReturnTrue_WhenJtiIsBlacklisted() {
        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user).token("raw.jwt").jti("jti-123")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        assertThat(blacklistedTokenRepository.existsByJti("jti-123")).isTrue();
    }

    @Test
    void _02_ShouldReturnFalse_WhenJtiIsNotBlacklisted() {
        assertThat(blacklistedTokenRepository.existsByJti("unknown-jti")).isFalse();
    }

    @Test
    void _03_ShouldRemoveOnlyExpiredEntries_WhenDeletingByExpiresAtBefore() {
        BlacklistedToken expired = blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user).token("old.jwt").jti("jti-old")
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());
        BlacklistedToken active = blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user).token("new.jwt").jti("jti-new")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        blacklistedTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

        assertThat(blacklistedTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(blacklistedTokenRepository.findById(active.getId())).isPresent();
    }
}
