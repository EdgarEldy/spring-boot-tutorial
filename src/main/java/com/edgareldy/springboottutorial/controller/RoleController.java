package com.edgareldy.springboottutorial.controller;

import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import com.edgareldy.springboottutorial.dto.role.RoleRequest;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD endpoints for roles and role/permission
 * assignment under {@code /api/v1/roles}. Delegates every operation to
 * {@link RoleService}; no business logic lives here.
 * Every endpoint is ADMIN-only.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "List roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(roleService.findAll(), "Roles retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a role")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roleService.create(request), "Role created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.update(id, request), "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Assign a permission to a role")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(ApiResponse.success(
                roleService.assignPermission(roleId, permissionId), "Permission assigned successfully"));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Remove a permission from a role")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(ApiResponse.success(
                roleService.removePermission(roleId, permissionId), "Permission removed successfully"));
    }
}
