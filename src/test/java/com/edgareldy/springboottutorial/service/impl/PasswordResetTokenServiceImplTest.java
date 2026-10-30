package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.repository.PasswordResetTokenRepository;
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
 * Unit tests for {@link PasswordResetTokenServiceImpl}, with
 * {@link PasswordResetTokenRepository} and {@link SecureTokenGenerator}
 * mocked.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private PasswordResetTokenServiceImpl passwordResetTokenService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).email("ada@example.com").username("ada").build();
    }

    @Test
    void _01_ShouldSaveTokenAndReturnRawValue_WhenTokenIsGenerated() {
        when(secureTokenGenerator.generate()).thenReturn("raw-token");

        String result = passwordResetTokenService.generate(user);

        assertThat(result).isEqualTo("raw-token");
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("raw-token");
        assertThat(captor.getValue().getType()).isEqualTo("PASSWORD_RESET");
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void _02_ShouldDeleteTokenAndReturnUser_WhenTokenIsValid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("raw-token").type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        when(passwordResetTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        AppUser result = passwordResetTokenService.validateAndConsume("raw-token");

        assertThat(result).isEqualTo(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void _03_ShouldThrowException_WhenTokenIsMissing() {
        when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("missing"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordResetTokenRepository, never()).delete(any());
    }

    @Test
    void _04_ShouldThrowExceptionAndStillDeleteToken_WhenTokenIsExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("raw-token").type("PASSWORD_RESET")
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        when(passwordResetTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("raw-token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordResetTokenRepository).delete(token);
    }
}
