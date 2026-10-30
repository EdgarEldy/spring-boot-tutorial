package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Spring Data JPA repository for {@link PasswordResetToken}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Acquires a pessimistic write lock so two concurrent validateAndConsume
     * calls for the same token cannot both read the row before either has
     * deleted it, which would allow the token to be used twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByExpiryDateBefore(Instant instant);
}
