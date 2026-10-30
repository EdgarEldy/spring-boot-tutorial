package com.edgareldy.springboottutorial.security;

import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Generates and parses the JWTs issued on login and consumed by
 * {@link JwtAuthFilter} on every subsequent request. Claims: {@code sub}
 * (username), {@code jti} (random UUID, used for blacklisting on logout),
 * {@code roles} (list of role names), {@code permissions} (list of
 * RESOURCE:ACTION strings), {@code iat}, {@code exp}. The signing key and
 * expiration come from {@link JwtProperties} ({@code jwt.secret}/{@code
 * jwt.expiration-ms}).
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes());
        this.expirationMs = jwtProperties.expirationMs();
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .map(name -> name.toUpperCase(Locale.ROOT))
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getResource().toUpperCase(Locale.ROOT)
                        + ":" + p.getAction().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
