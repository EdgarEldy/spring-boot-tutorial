package com.edgareldy.springboottutorial.scheduler;

import com.edgareldy.springboottutorial.repository.ActivationTokenRepository;
import com.edgareldy.springboottutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springboottutorial.repository.PasswordResetTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job that purges expired token rows so the three token tables do
 * not grow unbounded. Each table is purged by its own method with its own
 * transaction: a failure purging one table (e.g. a lock timeout) does not
 * roll back the others.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);
    private static final String DAILY_AT_3AM = "0 0 3 * * *";

    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Scheduled(cron = DAILY_AT_3AM)
    @Transactional
    public void purgeExpiredActivationTokens() {
        activationTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
        log.info("Purged expired activation tokens");
    }

    @Scheduled(cron = DAILY_AT_3AM)
    @Transactional
    public void purgeExpiredPasswordResetTokens() {
        passwordResetTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
        log.info("Purged expired password reset tokens");
    }

    @Scheduled(cron = DAILY_AT_3AM)
    @Transactional
    public void purgeExpiredBlacklistedTokens() {
        blacklistedTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
        log.info("Purged expired blacklisted tokens");
    }
}
