package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} for {@link AppUserRepository}, backed by a real
 * PostgreSQL instance via Testcontainers.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestcontainersConfiguration.class)
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        appUserRepository.save(AppUser.builder()
                .username("ada").email("ada@example.com").password("hashed-password").enabled(true).build());
    }

    @Test
    void _01_ShouldReturnMatchingUser_WhenFindingByUsername() {
        assertThat(appUserRepository.findByUsername("ada"))
                .isPresent()
                .get()
                .extracting(AppUser::getEmail)
                .isEqualTo("ada@example.com");
    }

    @Test
    void _02_ShouldReturnEmpty_WhenUsernameMissing() {
        assertThat(appUserRepository.findByUsername("unknown")).isEmpty();
    }

    @Test
    void _03_ShouldReflectCurrentData_WhenCheckingUsernameExists() {
        assertThat(appUserRepository.existsByUsername("ada")).isTrue();
        assertThat(appUserRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void _04_ShouldMatchEmail_WhenCaseDiffers() {
        assertThat(appUserRepository.existsByEmailIgnoreCase("ADA@EXAMPLE.COM")).isTrue();
        assertThat(appUserRepository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();
    }

    @Test
    void _05_ShouldFindUserByEmail_WhenCaseDiffers() {
        assertThat(appUserRepository.findByEmailIgnoreCase("ADA@EXAMPLE.COM"))
                .isPresent()
                .get()
                .extracting(AppUser::getUsername)
                .isEqualTo("ada");
        assertThat(appUserRepository.findByEmailIgnoreCase("unknown@example.com")).isEmpty();
    }
}
