package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
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
 * {@code @DataJpaTest} for {@link PasswordResetTokenRepository}, backed by a
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
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

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
    void _01_ShouldReturnMatchingToken_WhenFindingByToken() {
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user).token("reset-token").type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        assertThat(passwordResetTokenRepository.findByToken("reset-token"))
                .isPresent()
                .get()
                .extracting(PasswordResetToken::getUser)
                .isEqualTo(user);
    }

    @Test
    void _02_ShouldReturnEmpty_WhenTokenIsMissing() {
        assertThat(passwordResetTokenRepository.findByToken("unknown")).isEmpty();
    }

    @Test
    void _03_ShouldRemoveOnlyExpiredTokens_WhenDeletingByExpiryDateBefore() {
        PasswordResetToken expired = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user).token("expired-token").type("PASSWORD_RESET")
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());
        PasswordResetToken active = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user).token("active-token").type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        passwordResetTokenRepository.deleteAllByExpiryDateBefore(Instant.now());

        assertThat(passwordResetTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(passwordResetTokenRepository.findById(active.getId())).isPresent();
    }
}
