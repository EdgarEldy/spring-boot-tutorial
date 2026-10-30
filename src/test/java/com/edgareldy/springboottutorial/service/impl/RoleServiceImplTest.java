package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link RoleServiceImpl}, with {@link RoleRepository},
 * {@link PermissionRepository}, and {@link RoleMapper} mocked.
 * Note: there is no user-assignment guard in delete() at this stage;
 * that check will be added in feature/user-management once AppUser gains
 * a Set of Role.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private Permission permission;
    private RoleResponse roleResponse;

    @BeforeEach
    void setUp() {
        role = Role.builder().id(1L).roleName("ADMIN").permissions(new HashSet<>()).build();
        permission = Permission.builder().id(1L).resource("PRODUCT").action("WRITE").build();
        roleResponse = new RoleResponse(1L, "ADMIN", List.of());
    }

    @Test
    void _01_ShouldReturnMappedPage_WhenListingRoles() {
        Pageable pageable = Pageable.ofSize(20);
        when(roleRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(role)));
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        PageResponse<RoleResponse> result = roleService.findAll(pageable);
        assertThat(result.content()).containsExactly(roleResponse);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void _02_ShouldSaveRole_WhenRoleNameUnused() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.existsByRoleNameIgnoreCase("ADMIN")).thenReturn(false);
        when(roleMapper.toEntity(request)).thenReturn(role);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        assertThat(roleService.create(request)).isEqualTo(roleResponse);
    }

    @Test
    void _03_ShouldThrowBusinessRule_WhenRoleNameAlreadyExists() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.existsByRoleNameIgnoreCase("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> roleService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _04_ShouldApplyNewName_WhenRoleNameUnused() {
        RoleRequest request = new RoleRequest("MODERATOR");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByRoleNameIgnoreCase("MODERATOR")).thenReturn(false);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(new RoleResponse(1L, "MODERATOR", List.of()));

        assertThat(roleService.update(1L, request).roleName()).isEqualTo("MODERATOR");
    }

    @Test
    void _05_ShouldSkipUniquenessCheck_WhenNameUnchanged() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        assertThat(roleService.update(1L, request)).isEqualTo(roleResponse);

        verify(roleRepository, never()).existsByRoleNameIgnoreCase(any());
    }

    @Test
    void _06_ShouldThrowBusinessRule_WhenNewNameUsedByAnotherRole() {
        RoleRequest request = new RoleRequest("MODERATOR");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByRoleNameIgnoreCase("MODERATOR")).thenReturn(true);

        assertThatThrownBy(() -> roleService.update(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _07_ShouldDeleteRole_WhenRoleExists() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        roleService.delete(1L);

        verify(roleRepository).delete(role);
    }

    @Test
    void _08_ShouldThrowNotFound_WhenDeletingMissingRole() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _09_ShouldAddPermission_WhenPermissionNotAlreadyAssigned() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        roleService.assignPermission(1L, 1L);

        assertThat(role.getPermissions()).contains(permission);
    }

    @Test
    void _10_ShouldThrowBusinessRule_WhenPermissionAlreadyAssigned() {
        role.getPermissions().add(permission);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        assertThatThrownBy(() -> roleService.assignPermission(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _11_ShouldThrowNotFound_WhenAssigningMissingPermission() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignPermission(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _12_ShouldThrowNotFound_WhenAssigningPermissionToMissingRole() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignPermission(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(permissionRepository, never()).findById(any());
    }

    @Test
    void _13_ShouldRemovePermission_WhenPermissionIsAssigned() {
        role.getPermissions().add(permission);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        roleService.removePermission(1L, 1L);

        assertThat(role.getPermissions()).doesNotContain(permission);
    }

    @Test
    void _14_ShouldThrowBusinessRule_WhenPermissionNotAssigned() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        assertThatThrownBy(() -> roleService.removePermission(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void _15_ShouldThrowNotFound_WhenRemovingPermissionFromMissingRole() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.removePermission(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(permissionRepository, never()).findById(any());
    }
}
