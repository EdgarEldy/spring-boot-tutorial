package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.token.ActivationToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Spring Data JPA repository for {@link ActivationToken}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {

    /**
     * Acquires a pessimistic write lock so two concurrent validate calls for
     * the same token cannot both read {@code validatedAt == null} before
     * either sets it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivationToken> findByToken(String token);

    void deleteAllByExpiresAtBefore(Instant instant);
}
