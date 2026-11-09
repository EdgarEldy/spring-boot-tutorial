package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.token.RefreshToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.exception.TokenExpiredException;
import com.edgareldy.springboottutorial.repository.RefreshTokenRepository;
import com.edgareldy.springboottutorial.security.JwtProperties;
import com.edgareldy.springboottutorial.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RefreshTokenServiceImpl}, with
 * {@link RefreshTokenRepository}, {@link JwtService}, and
 * {@link JwtProperties} mocked.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).username("ada").email("ada@example.com").build();
    }

    @Test
    void _01_ShouldPurgePreviousTokensThenSave_WhenGenerating() {
        when(jwtService.generateRefreshToken("ada")).thenReturn("raw-refresh-token");
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.generate(user);

        verify(refreshTokenRepository).deleteByUser(user);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("raw-refresh-token");
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getToken()).isEqualTo("raw-refresh-token");
    }

    @Test
    void _02_ShouldReturnToken_WhenValidAndNotExpired() {
        RefreshToken token = RefreshToken.builder()
                .token("raw-refresh-token").user(user)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByToken("raw-refresh-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validate("raw-refresh-token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    void _03_ShouldThrowInvalidTokenException_WhenTokenNotFound() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("missing-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _04_ShouldThrowTokenExpiredException_WhenTokenIsExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("old-refresh-token").user(user)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validate("old-refresh-token"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void _05_ShouldDelegateToRepository_WhenDeleteByUserCalled() {
        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
