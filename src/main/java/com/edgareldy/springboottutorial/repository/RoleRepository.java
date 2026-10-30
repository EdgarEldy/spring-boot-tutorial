package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Role}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Overrides the base paginated find-all to eagerly fetch each role's
     * permissions, avoiding one extra select per row when the controller
     * maps every role to a {@code RoleResponse} (which reads
     * {@code permissions}).
     */
    @EntityGraph(attributePaths = "permissions")
    @Override
    Page<Role> findAll(Pageable pageable);

    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);

    boolean existsByPermissions_Id(Long permissionId);
}
