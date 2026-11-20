package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.role.RoleRequest;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.dto.role.UpdateRolePermissionsRequest;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link com.edgareldy.springboottutorial.entity.Role} business
 * operations, including permission assignment. Controllers and tests depend
 * on this interface, never on its implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface RoleService {

    PageResponse<RoleResponse> findAll(Pageable pageable);

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long id, RoleRequest request);

    void delete(Long id);

    /**
     * Adds and removes permissions on a role in a single transaction. Adding a
     * permission the role already has, or removing one it does not have, is a
     * no-op rather than an error.
     */
    RoleResponse updatePermissions(Long roleId, UpdateRolePermissionsRequest request);
}
