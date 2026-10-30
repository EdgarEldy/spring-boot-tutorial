package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.token.BlacklistedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link BlacklistedToken}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByJti(String jti);

    void deleteAllByExpiresAtBefore(Instant instant);
}
