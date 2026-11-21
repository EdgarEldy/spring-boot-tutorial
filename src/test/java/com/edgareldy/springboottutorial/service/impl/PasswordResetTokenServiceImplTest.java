package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import com.edgareldy.springboottutorial.entity.token.PasswordResetTokenType;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
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
 * {@link PasswordResetTokenRepository}, {@link AppUserRepository} and
 * {@link SecureTokenGenerator} mocked.
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
    private AppUserRepository appUserRepository;

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
    void _01_ShouldGenerateLinkTokenWithUuid_WhenTypeIsLink() {
        when(secureTokenGenerator.generate()).thenReturn("uuid-token");

        String result = passwordResetTokenService.generate(user, PasswordResetTokenType.LINK);

        assertThat(result).isEqualTo("uuid-token");
        verify(passwordResetTokenRepository).deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("uuid-token");
        assertThat(captor.getValue().getType()).isEqualTo(PasswordResetTokenType.LINK);
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void _02_ShouldGenerateOtpTokenWithSixDigits_WhenTypeIsOtp() {
        when(secureTokenGenerator.generateOtp(6)).thenReturn("654321");

        String result = passwordResetTokenService.generate(user, PasswordResetTokenType.OTP);

        assertThat(result).isEqualTo("654321");
        verify(passwordResetTokenRepository).deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("654321");
        assertThat(captor.getValue().getType()).isEqualTo(PasswordResetTokenType.OTP);
        // OTP TTL is 5 minutes - verify expiry is in the future but well within 10 minutes
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());
        assertThat(captor.getValue().getExpiryDate()).isBefore(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    @Test
    void _03_ShouldDeleteTokenAndReturnUser_WhenTokenIsValid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("uuid-token").type(PasswordResetTokenType.LINK)
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        when(passwordResetTokenRepository.findByToken("uuid-token")).thenReturn(Optional.of(token));

        AppUser result = passwordResetTokenService.validateAndConsume("uuid-token");

        assertThat(result).isEqualTo(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void _04_ShouldThrowException_WhenTokenIsMissing() {
        when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("missing"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordResetTokenRepository, never()).delete(any());
    }

    @Test
    void _05_ShouldThrowExceptionAndCallDelete_WhenTokenIsExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("uuid-token").type(PasswordResetTokenType.LINK)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        when(passwordResetTokenRepository.findByToken("uuid-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetTokenService.validateAndConsume("uuid-token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void _06_verifyOtp_ShouldReturnUser_WhenOtpIsValid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("123456").type(PasswordResetTokenType.OTP)
                .expiryDate(Instant.now().plus(3, ChronoUnit.MINUTES))
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(token));

        AppUser result = passwordResetTokenService.verifyOtp("ada@example.com", "123456");

        assertThat(result).isEqualTo(user);
        verify(passwordResetTokenRepository, never()).delete(any());
    }

    @Test
    void _07_verifyOtp_ShouldThrow_WhenOtpIsNotFound() {
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndToken(user, "000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetTokenService.verifyOtp("ada@example.com", "000000"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _08_verifyOtp_ShouldThrow_WhenTokenIsNotOtpType() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("123456").type(PasswordResetTokenType.LINK)
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetTokenService.verifyOtp("ada@example.com", "123456"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _09_verifyOtp_ShouldThrow_WhenOtpIsExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L).user(user).token("123456").type(PasswordResetTokenType.OTP)
                .expiryDate(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndToken(user, "123456")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetTokenService.verifyOtp("ada@example.com", "123456"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void _10_verifyOtp_ShouldThrow_WhenUserEmailNotFound() {
        when(appUserRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetTokenService.verifyOtp("unknown@example.com", "123456"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(passwordResetTokenRepository, never()).findByUserAndToken(any(), any());
    }
}
