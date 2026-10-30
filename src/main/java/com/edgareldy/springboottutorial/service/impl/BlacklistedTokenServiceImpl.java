package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.entity.token.BlacklistedToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link BlacklistedTokenService} implementation backed by
 * {@link BlacklistedTokenRepository}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class BlacklistedTokenServiceImpl implements BlacklistedTokenService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    @Transactional
    public void blacklist(AppUser user, String rawJwt, String jti, Instant expiresAt) {
        Instant now = Instant.now();
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .user(user)
                .token(rawJwt)
                .jti(jti)
                .blacklistedAt(now)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
        blacklistedTokenRepository.save(blacklistedToken);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return blacklistedTokenRepository.existsByJti(jti);
    }
}
