package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import com.edgareldy.springboottutorial.entity.token.PasswordResetTokenType;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.PasswordResetTokenRepository;
import com.edgareldy.springboottutorial.security.SecureTokenGenerator;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PasswordResetTokenService} implementation. Generates either
 * a UUID token (LINK, 1-hour TTL) for the web flow or a 6-digit OTP (OTP,
 * 5-minute TTL) for the mobile flow. In {@link #validateAndConsume}, the row
 * is deleted before the expiry check: if the token is expired the rollback
 * leaves it in place until {@code TokenCleanupScheduler} removes it.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private static final Duration LINK_EXPIRATION = Duration.ofHours(1);
    private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AppUserRepository appUserRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public String generate(AppUser user, PasswordResetTokenType type) {
        passwordResetTokenRepository.deleteByUser(user);
        String rawToken = type == PasswordResetTokenType.OTP
                ? secureTokenGenerator.generateOtp(OTP_LENGTH)
                : secureTokenGenerator.generate();
        Duration ttl = type == PasswordResetTokenType.OTP ? OTP_EXPIRATION : LINK_EXPIRATION;
        Instant now = Instant.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(rawToken)
                .createdAt(now)
                .type(type)
                .expiryDate(now.plus(ttl))
                .build();
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional
    public AppUser validateAndConsume(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));
        passwordResetTokenRepository.delete(resetToken);
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token expired");
        }
        return resetToken.getUser();
    }

    @Override
    @Transactional
    public AppUser verifyOtp(String email, String otp) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserAndToken(user, otp)
                .orElseThrow(() -> new InvalidTokenException("Invalid OTP"));
        if (resetToken.getType() != PasswordResetTokenType.OTP) {
            throw new InvalidTokenException("Invalid OTP");
        }
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("OTP expired");
        }
        return user;
    }
}
