package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import java.time.Instant;

/**
 * Contract for JWT revocation on logout. {@code JwtAuthFilter} calls
 * {@link #isBlacklisted} on every authenticated request; {@code AuthServiceImpl}
 * calls {@link #blacklist} when the user logs out.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface BlacklistedTokenService {

    void blacklist(AppUser user, String rawJwt, String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);
}
