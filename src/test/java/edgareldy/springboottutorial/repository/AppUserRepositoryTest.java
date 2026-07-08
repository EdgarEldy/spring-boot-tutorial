package edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import edgareldy.springboottutorial.entity.user.AppUser;
import edgareldy.springboottutorial.entity.user.Role;
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
                .username("ada").email("ada@example.com").password("hashed-password").role(Role.USER).build());
    }

    @Test
    void findByUsernameReturnsMatchingUser() {
        assertThat(appUserRepository.findByUsername("ada"))
                .isPresent()
                .get()
                .extracting(AppUser::getEmail)
                .isEqualTo("ada@example.com");
    }

    @Test
    void findByUsernameReturnsEmptyWhenMissing() {
        assertThat(appUserRepository.findByUsername("unknown")).isEmpty();
    }

    @Test
    void existsByUsernameReflectsCurrentData() {
        assertThat(appUserRepository.existsByUsername("ada")).isTrue();
        assertThat(appUserRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void existsByEmailIgnoreCaseMatchesRegardlessOfCase() {
        assertThat(appUserRepository.existsByEmailIgnoreCase("ADA@EXAMPLE.COM")).isTrue();
        assertThat(appUserRepository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();
    }
}
