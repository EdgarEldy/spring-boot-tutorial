package com.edgareldy.springboottutorial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} for {@link RoleRepository}, backed by a real
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Permission permission;

    @BeforeEach
    void setUp() {
        // ADMIN is already seeded by Flyway V7. Add a test permission to it so
        // _03 can verify existsByPermissions_Id on a known assignment.
        permission = permissionRepository.save(Permission.builder().resource("PRODUCT").action("WRITE").build());
        Role admin = roleRepository.findByRoleNameIgnoreCase("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Bootstrap role ADMIN not found"));
        admin.getPermissions().add(permission);
        roleRepository.save(admin);
    }

    @Test
    void _01_ShouldReturnMatchingRole_WhenFindingByRoleName() {
        assertThat(roleRepository.findByRoleNameIgnoreCase("admin"))
                .isPresent()
                .get()
                .extracting(Role::getRoleName)
                .isEqualTo("ADMIN");
    }

    @Test
    void _02_ShouldReflectCurrentData_WhenCheckingRoleNameExists() {
        assertThat(roleRepository.existsByRoleNameIgnoreCase("ADMIN")).isTrue();
        assertThat(roleRepository.existsByRoleNameIgnoreCase("unknown")).isFalse();
    }

    @Test
    void _03_ShouldReturnTrue_WhenPermissionIsAssignedToRole() {
        assertThat(roleRepository.existsByPermissions_Id(permission.getId())).isTrue();
    }

    @Test
    void _04_ShouldReturnFalse_WhenPermissionIsUnassigned() {
        Permission other = permissionRepository.save(Permission.builder().resource("USER").action("DELETE").build());

        assertThat(roleRepository.existsByPermissions_Id(other.getId())).isFalse();
    }
}
