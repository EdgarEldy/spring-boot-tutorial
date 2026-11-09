package com.edgareldy.springboottutorial.entity.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA entity mapping the {@code blacklisted_tokens} table. Recorded by
 * {@code BlacklistedTokenService} when a user logs out, so that
 * {@code JwtAuthFilter} can reject a JWT by its {@code jti} claim even
 * though the token itself has not expired yet.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BlacklistedToken extends AuditableToken {

    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @Column(name = "blacklisted_at")
    private Instant blacklistedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
