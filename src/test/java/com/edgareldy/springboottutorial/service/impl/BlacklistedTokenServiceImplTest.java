package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.token.BlacklistedToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.BlacklistedTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link BlacklistedTokenServiceImpl}, with
 * {@link BlacklistedTokenRepository} mocked.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class BlacklistedTokenServiceImplTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private BlacklistedTokenServiceImpl blacklistedTokenService;

    @Test
    void _01_ShouldSaveEntryWithJtiAndExpiry_WhenTokenIsBlacklisted() {
        AppUser user = AppUser.builder().id(1L).email("ada@example.com").username("ada").build();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        blacklistedTokenService.blacklist(user, "raw.jwt.value", "jti-123", expiresAt);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("raw.jwt.value");
        assertThat(captor.getValue().getJti()).isEqualTo("jti-123");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(captor.getValue().getBlacklistedAt()).isNotNull();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void _02_ShouldDelegateToRepository_WhenBlacklistStatusIsChecked() {
        when(blacklistedTokenRepository.existsByJti("jti-123")).thenReturn(true);
        when(blacklistedTokenRepository.existsByJti("jti-456")).thenReturn(false);

        assertThat(blacklistedTokenService.isBlacklisted("jti-123")).isTrue();
        assertThat(blacklistedTokenService.isBlacklisted("jti-456")).isFalse();
    }
}
