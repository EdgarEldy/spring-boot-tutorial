package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.token.RefreshToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RefreshToken}.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(AppUser user);

    void deleteAllByExpiresAtBefore(Instant instant);
}
