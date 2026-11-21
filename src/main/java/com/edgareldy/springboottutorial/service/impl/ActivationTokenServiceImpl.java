package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.token.ActivationToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.exception.TokenExpiredException;
import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.security.SecureTokenGenerator;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ActivationTokenService} implementation backed by
 * {@link ActivationTokenRepository}. Generates 6-digit numeric OTPs
 * with a 15-minute TTL. Validation is scoped to the requesting user
 * (email + OTP) so two accounts that happen to receive the same code
 * cannot activate each other.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class ActivationTokenServiceImpl implements ActivationTokenService {

    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private static final int OTP_LENGTH = 6;

    private final ActivationTokenRepository activationTokenRepository;
    private final AppUserRepository appUserRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public String generate(AppUser user) {
        activationTokenRepository.deleteByUser(user);
        String otp = secureTokenGenerator.generateOtp(OTP_LENGTH);
        Instant now = Instant.now();
        ActivationToken activationToken = ActivationToken.builder()
                .user(user)
                .token(otp)
                .createdAt(now)
                .expiresAt(now.plus(EXPIRATION))
                .build();
        activationTokenRepository.save(activationToken);
        return otp;
    }

    @Override
    @Transactional
    public AppUser validate(String email, String otp) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
        ActivationToken activationToken = activationTokenRepository.findByUserAndToken(user, otp)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation code"));
        if (activationToken.getValidatedAt() != null) {
            throw new InvalidTokenException("Activation code already used");
        }
        if (activationToken.getExpiresAt() != null
                && activationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Activation code expired");
        }
        activationToken.setValidatedAt(Instant.now());
        activationTokenRepository.save(activationToken);
        return user;
    }
}
