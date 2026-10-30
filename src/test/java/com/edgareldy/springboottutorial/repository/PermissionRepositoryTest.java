package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} for {@link PermissionRepository}, backed by a real
 * PostgreSQL instance via Testcontainers.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestcontainersConfiguration.class)
class PermissionRepositoryTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @BeforeEach
    void setUp() {
        permissionRepository.save(Permission.builder().resource("PRODUCT").action("WRITE").build());
    }

    @Test
    void _01_ShouldMatchPair_WhenCaseDiffers() {
        assertThat(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("product", "write")).isTrue();
    }

    @Test
    void _02_ShouldReturnFalse_WhenPairHasNoMatch() {
        assertThat(permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase("USER", "DELETE")).isFalse();
    }
}
