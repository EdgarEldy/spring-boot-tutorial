package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.role.RoleRequest;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.RoleMapper;
import com.edgareldy.springboottutorial.repository.PermissionRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link RoleService} implementation backed by {@link RoleRepository}.
 * The guard "role is assigned to at least one user" is intentionally absent
 * from {@code delete()}: the role_user join table does not exist yet. It will
 * be added in feature/user-management (V7) once AppUser gains a Set of Role.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    public PageResponse<RoleResponse> findAll(Pageable pageable) {
        return PageResponse.from(roleRepository.findAll(pageable).map(roleMapper::toResponse));
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByRoleNameIgnoreCase(request.roleName())) {
            throw new BusinessRuleException("Role " + request.roleName() + " already exists");
        }
        Role role = roleMapper.toEntity(request);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = getRoleOrThrow(id);
        if (!role.getRoleName().equalsIgnoreCase(request.roleName())
                && roleRepository.existsByRoleNameIgnoreCase(request.roleName())) {
            throw new BusinessRuleException("Role " + request.roleName() + " already exists");
        }
        role.setRoleName(request.roleName());
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = getRoleOrThrow(id);
        roleRepository.delete(role);
    }

    @Override
    @Transactional
    public RoleResponse assignPermission(Long roleId, Long permissionId) {
        Role role = getRoleOrThrow(roleId);
        Permission permission = getPermissionOrThrow(permissionId);
        if (!role.getPermissions().add(permission)) {
            throw new BusinessRuleException(
                    "Permission " + permissionId + " is already assigned to role " + roleId);
        }
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse removePermission(Long roleId, Long permissionId) {
        Role role = getRoleOrThrow(roleId);
        Permission permission = getPermissionOrThrow(permissionId);
        if (!role.getPermissions().remove(permission)) {
            throw new BusinessRuleException(
                    "Permission " + permissionId + " is not assigned to role " + roleId);
        }
        return roleMapper.toResponse(roleRepository.save(role));
    }

    private Role getRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));
    }

    private Permission getPermissionOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));
    }
}
