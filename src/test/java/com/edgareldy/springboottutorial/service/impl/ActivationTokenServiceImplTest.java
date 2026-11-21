package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.token.ActivationToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.exception.TokenExpiredException;
import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.security.SecureTokenGenerator;
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
 * Unit tests for {@link ActivationTokenServiceImpl}, with
 * {@link ActivationTokenRepository}, {@link AppUserRepository} and
 * {@link SecureTokenGenerator} mocked.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ActivationTokenServiceImplTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private ActivationTokenServiceImpl activationTokenService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).email("ada@example.com").username("ada").build();
    }

    @Test
    void _01_ShouldPurgePreviousTokensThenSave_WhenOtpIsGenerated() {
        when(secureTokenGenerator.generateOtp(6)).thenReturn("123456");

        String result = activationTokenService.generate(user);

        assertThat(result).isEqualTo("123456");
        verify(activationTokenRepository).deleteByUser(user);
        ArgumentCaptor<ActivationToken> captor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("123456");
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(captor.getValue().getValidatedAt()).isNull();
    }

    @Test
    void _02_ShouldMarkTokenValidatedAndReturnUser_WhenOtpIsValid() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("123456")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(activationTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(activationToken));

        AppUser result = activationTokenService.validate("ada@example.com", "123456");

        assertThat(result).isEqualTo(user);
        assertThat(activationToken.getValidatedAt()).isNotNull();
        verify(activationTokenRepository).save(activationToken);
    }

    @Test
    void _03_ShouldThrowException_WhenOtpIsNotFound() {
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(activationTokenRepository.findByUserAndToken(user, "000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationTokenService.validate("ada@example.com", "000000"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _04_ShouldThrowException_WhenOtpIsAlreadyValidated() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("123456")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .validatedAt(Instant.now())
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(activationTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(activationToken));

        assertThatThrownBy(() -> activationTokenService.validate("ada@example.com", "123456"))
                .isInstanceOf(InvalidTokenException.class);

        verify(activationTokenRepository, never()).save(any());
    }

    @Test
    void _05_ShouldThrowTokenExpiredException_WhenOtpIsExpired() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("123456")
                .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(activationTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(activationToken));

        assertThatThrownBy(() -> activationTokenService.validate("ada@example.com", "123456"))
                .isInstanceOf(TokenExpiredException.class);
    }
}
