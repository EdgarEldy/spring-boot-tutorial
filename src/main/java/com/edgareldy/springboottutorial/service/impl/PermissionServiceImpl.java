package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.permission.PermissionRequest;
import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.PermissionMapper;
import com.edgareldy.springboottutorial.repository.PermissionRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.service.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PermissionService} implementation backed by
 * {@link PermissionRepository}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream().map(permissionMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByResourceIgnoreCaseAndActionIgnoreCase(request.resource(), request.action())) {
            throw new BusinessRuleException(
                    "Permission " + request.resource() + ":" + request.action() + " already exists");
        }
        Permission permission = permissionMapper.toEntity(request);
        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));
        if (roleRepository.existsByPermissions_Id(id)) {
            throw new BusinessRuleException(
                    "Permission " + id + " is assigned to at least one role and cannot be deleted");
        }
        permissionRepository.delete(permission);
    }
}
