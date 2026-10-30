package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.repository.PasswordResetTokenRepository;
import com.edgareldy.springboottutorial.security.SecureTokenGenerator;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PasswordResetTokenService} implementation backed by
 * {@link PasswordResetTokenRepository}. The token is deleted as soon as it
 * is looked up in {@link #validateAndConsume} so it cannot be replayed even
 * if the expiry check is slow.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public String generate(AppUser user) {
        String rawToken = secureTokenGenerator.generate();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .token(rawToken)
                .type(TYPE_PASSWORD_RESET)
                .expiryDate(Instant.now().plus(EXPIRATION))
                .build();
        passwordResetTokenRepository.save(passwordResetToken);
        return rawToken;
    }

    @Override
    @Transactional
    public AppUser validateAndConsume(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));
        passwordResetTokenRepository.delete(passwordResetToken);
        if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token expired");
        }
        return passwordResetToken.getUser();
    }
}
