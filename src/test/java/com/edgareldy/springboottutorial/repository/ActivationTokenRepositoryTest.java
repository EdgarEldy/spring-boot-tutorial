package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.token.ActivationToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} for {@link ActivationTokenRepository}, backed by a
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
class ActivationTokenRepositoryTest {

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = appUserRepository.save(AppUser.builder()
                .username("ada").email("ada@example.com")
                .password("hashed").enabled(true).build());
    }

    @Test
    void _01_ShouldReturnMatchingToken_WhenFindingByToken() {
        activationTokenRepository.save(ActivationToken.builder()
                .user(user).token("raw-token")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        assertThat(activationTokenRepository.findByToken("raw-token"))
                .isPresent()
                .get()
                .extracting(ActivationToken::getUser)
                .isEqualTo(user);
    }

    @Test
    void _02_ShouldReturnEmpty_WhenTokenIsMissing() {
        assertThat(activationTokenRepository.findByToken("unknown")).isEmpty();
    }

    @Test
    void _03_ShouldRemoveOnlyExpiredTokens_WhenDeletingByExpiresAtBefore() {
        ActivationToken expired = activationTokenRepository.save(ActivationToken.builder()
                .user(user).token("expired-token")
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());
        ActivationToken active = activationTokenRepository.save(ActivationToken.builder()
                .user(user).token("active-token")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        activationTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

        assertThat(activationTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(activationTokenRepository.findById(active.getId())).isPresent();
    }
}
