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
import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
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
 * {@link ActivationTokenRepository} and {@link SecureTokenGenerator} mocked.
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
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private ActivationTokenServiceImpl activationTokenService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).email("ada@example.com").username("ada").build();
    }

    @Test
    void _01_ShouldSaveTokenAndReturnRawValue_WhenTokenIsGenerated() {
        when(secureTokenGenerator.generate()).thenReturn("raw-token");

        String result = activationTokenService.generate(user);

        assertThat(result).isEqualTo("raw-token");
        ArgumentCaptor<ActivationToken> captor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("raw-token");
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(captor.getValue().getValidatedAt()).isNull();
    }

    @Test
    void _02_ShouldMarkTokenValidatedAndReturnUser_WhenTokenIsValid() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("raw-token")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(activationToken));

        AppUser result = activationTokenService.validate("raw-token");

        assertThat(result).isEqualTo(user);
        assertThat(activationToken.getValidatedAt()).isNotNull();
        verify(activationTokenRepository).save(activationToken);
    }

    @Test
    void _03_ShouldThrowException_WhenTokenIsMissing() {
        when(activationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationTokenService.validate("missing"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _04_ShouldThrowException_WhenTokenIsAlreadyValidated() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("raw-token")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .validatedAt(Instant.now())
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(activationToken));

        assertThatThrownBy(() -> activationTokenService.validate("raw-token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(activationTokenRepository, never()).save(any());
    }

    @Test
    void _05_ShouldThrowException_WhenTokenIsExpired() {
        ActivationToken activationToken = ActivationToken.builder()
                .id(1L).user(user).token("raw-token")
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(activationToken));

        assertThatThrownBy(() -> activationTokenService.validate("raw-token"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
