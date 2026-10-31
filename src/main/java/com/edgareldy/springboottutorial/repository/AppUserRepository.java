package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AppUser}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsername(String username);

    // Used by AuthServiceImpl.login() to resolve the Spring Security username
    // from the email the client supplies at login time.
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);
}
