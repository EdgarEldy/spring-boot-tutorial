package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.token.PasswordResetToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
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

    /**
     * Looks up a password-reset token by both owner and token value. Used by
     * the OTP verify step to scope the lookup to a specific user, preventing
     * two users who happen to share the same 6-digit OTP from verifying each
     * other's reset request.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByUserAndToken(AppUser user, String token);

    void deleteAllByExpiryDateBefore(Instant instant);

    void deleteByUser(AppUser user);
}
