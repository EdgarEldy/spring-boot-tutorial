package com.edgareldy.springboottutorial.entity.token;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Common mapped superclass for all token entities (activation, password-reset,
 * blacklisted JWT, refresh). Centralises the primary key, the owning user
 * relationship, the raw token string, and the creation timestamp so each
 * concrete entity only declares the fields that make it specific.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "id")
public abstract class AuditableToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token", nullable = false, length = 768)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
