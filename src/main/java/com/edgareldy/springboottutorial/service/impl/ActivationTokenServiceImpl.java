package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.token.ActivationToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.InvalidTokenException;
import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
import com.edgareldy.springboottutorial.security.SecureTokenGenerator;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ActivationTokenService} implementation backed by
 * {@link ActivationTokenRepository}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class ActivationTokenServiceImpl implements ActivationTokenService {

    private static final Duration EXPIRATION = Duration.ofHours(24);

    private final ActivationTokenRepository activationTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public String generate(AppUser user) {
        String rawToken = secureTokenGenerator.generate();
        Instant now = Instant.now();
        ActivationToken activationToken = ActivationToken.builder()
                .user(user)
                .token(rawToken)
                .createdAt(now)
                .expiresAt(now.plus(EXPIRATION))
                .build();
        activationTokenRepository.save(activationToken);
        return rawToken;
    }

    @Override
    @Transactional
    public AppUser validate(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));
        if (activationToken.getValidatedAt() != null) {
            throw new InvalidTokenException("Activation token already used");
        }
        if (activationToken.getExpiresAt() != null
                && activationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Activation token expired");
        }
        activationToken.setValidatedAt(Instant.now());
        activationTokenRepository.save(activationToken);
        return activationToken.getUser();
    }
}
