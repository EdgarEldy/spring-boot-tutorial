package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Permission}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByResourceIgnoreCaseAndActionIgnoreCase(String resource, String action);
}
