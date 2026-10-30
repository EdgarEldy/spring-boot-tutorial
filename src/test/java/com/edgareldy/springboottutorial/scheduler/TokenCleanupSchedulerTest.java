package com.edgareldy.springboottutorial.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
import com.edgareldy.springboottutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springboottutorial.repository.PasswordResetTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TokenCleanupScheduler}, with every repository mocked.
 * Each purge method is verified independently, including that calling one does
 * not touch the other repositories.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Test
    void _01_ShouldOnlyTouchActivationTokenRepository_WhenExpiredActivationTokensArePurged() {
        tokenCleanupScheduler.purgeExpiredActivationTokens();

        verify(activationTokenRepository).deleteAllByExpiresAtBefore(any(Instant.class));
        verify(passwordResetTokenRepository, never()).deleteAllByExpiryDateBefore(any());
        verify(blacklistedTokenRepository, never()).deleteAllByExpiresAtBefore(any());
    }

    @Test
    void _02_ShouldOnlyTouchPasswordResetTokenRepository_WhenExpiredPasswordResetTokensArePurged() {
        tokenCleanupScheduler.purgeExpiredPasswordResetTokens();

        verify(passwordResetTokenRepository).deleteAllByExpiryDateBefore(any(Instant.class));
        verify(activationTokenRepository, never()).deleteAllByExpiresAtBefore(any());
        verify(blacklistedTokenRepository, never()).deleteAllByExpiresAtBefore(any());
    }

    @Test
    void _03_ShouldOnlyTouchBlacklistedTokenRepository_WhenExpiredBlacklistedTokensArePurged() {
        tokenCleanupScheduler.purgeExpiredBlacklistedTokens();

        verify(blacklistedTokenRepository).deleteAllByExpiresAtBefore(any(Instant.class));
        verify(activationTokenRepository, never()).deleteAllByExpiresAtBefore(any());
        verify(passwordResetTokenRepository, never()).deleteAllByExpiryDateBefore(any());
    }
}
