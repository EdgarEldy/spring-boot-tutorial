package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.dto.user.AssignRolesRequest;
import com.edgareldy.springboottutorial.dto.user.UpdateProfileRequest;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link UserServiceImpl}, with all dependencies mocked.
 * Covers admin user management (findAll, findById, assignRoles, delete,
 * lock, unlock, updateProfile) and the three token-driven flows (activate,
 * forgotPassword, resetPassword).
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private AppUser appUser;
    private UserResponse userResponse;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).roleName("USER").build();
        appUser = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed")
                .enabled(false).roles(Set.of(userRole)).build();
        userResponse = new UserResponse(1L, "ada", "ada@example.com", false,
                List.of(new RoleResponse(1L, "USER", List.of())));
    }

    @Test
    void _01_ShouldReturnPageOfUsers_WhenFindingAll() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(appUserRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(appUser)));
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        PageResponse<UserResponse> result = userService.findAll(pageable);

        assertThat(result.content()).containsExactly(userResponse);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void _02_ShouldReturnUser_WhenFindingById() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        assertThat(userService.findById(1L)).isEqualTo(userResponse);
    }

    @Test
    void _03_ShouldThrowNotFound_WhenUserMissingOnFindById() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _04_ShouldAssignRoles_WhenAllRoleIdsValid() {
        Role adminRole = Role.builder().id(2L).roleName("ADMIN").build();
        AssignRolesRequest request = new AssignRolesRequest(Set.of(1L, 2L));
        AppUser saved = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed")
                .enabled(true).roles(Set.of(userRole, adminRole)).build();
        UserResponse savedResponse = new UserResponse(1L, "ada", "ada@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of()), new RoleResponse(2L, "ADMIN", List.of())));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(roleRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(userRole, adminRole));
        when(appUserRepository.save(appUser)).thenReturn(saved);
        when(appUserMapper.toResponse(saved)).thenReturn(savedResponse);

        assertThat(userService.assignRoles(1L, request)).isEqualTo(savedResponse);
    }

    @Test
    void _05_ShouldThrowBusinessRule_WhenSomeRoleIdsInvalid() {
        AssignRolesRequest request = new AssignRolesRequest(Set.of(1L, 99L));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(roleRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(userRole));

        assertThatThrownBy(() -> userService.assignRoles(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void _06_ShouldDeleteUser_WhenExists() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        userService.delete(1L);

        verify(appUserRepository).delete(appUser);
    }

    @Test
    void _07_ShouldThrowNotFound_WhenUserMissingOnDelete() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void _08_ShouldEnableUser_WhenActivationTokenValid() {
        when(activationTokenService.validate("tok")).thenReturn(appUser);
        AppUser enabled = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed")
                .enabled(true).roles(Set.of(userRole)).build();
        UserResponse enabledResponse = new UserResponse(1L, "ada", "ada@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(appUserRepository.save(appUser)).thenReturn(enabled);
        when(appUserMapper.toResponse(enabled)).thenReturn(enabledResponse);

        assertThat(userService.activate("tok")).isEqualTo(enabledResponse);
        assertThat(appUser.isEnabled()).isTrue();
    }

    @Test
    void _09_ShouldSendResetEmail_WhenEmailExists() {
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com"))
                .thenReturn(Optional.of(appUser));
        when(passwordResetTokenService.generate(appUser)).thenReturn("reset-token");

        userService.forgotPassword("ada@example.com");

        verify(emailService).sendPasswordResetEmail(eq(appUser), eq("reset-token"));
    }

    @Test
    void _10_ShouldDoNothing_WhenForgotPasswordEmailUnknown() {
        when(appUserRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        userService.forgotPassword("unknown@example.com");

        verify(passwordResetTokenService, never()).generate(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void _11_ShouldReHashPassword_WhenResetTokenValid() {
        when(passwordResetTokenService.validateAndConsume("reset-tok")).thenReturn(appUser);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hashed");

        userService.resetPassword("reset-tok", "new-password");

        assertThat(appUser.getPassword()).isEqualTo("new-hashed");
        verify(appUserRepository).save(appUser);
    }

    @Test
    void _12_ShouldLockUser_WhenCallerIsNotTarget() {
        AppUser target = AppUser.builder()
                .id(2L).username("target").email("target@example.com").password("hashed")
                .enabled(true).roles(Set.of(userRole)).build();
        UserResponse lockedResponse = new UserResponse(2L, "target", "target@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(target));
        when(appUserRepository.save(target)).thenReturn(target);
        when(appUserMapper.toResponse(target)).thenReturn(lockedResponse);

        UserResponse result = userService.lock(2L, "admin");

        assertThat(result).isEqualTo(lockedResponse);
        assertThat(target.isAccountLocked()).isTrue();
    }

    @Test
    void _13_ShouldThrowBusinessRule_WhenAdminLocksOwnAccount() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        assertThatThrownBy(() -> userService.lock(1L, "ada"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Admin cannot lock their own account");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void _14_ShouldUnlockUser_WhenCalled() {
        appUser.setAccountLocked(true);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(appUserRepository.save(appUser)).thenReturn(appUser);
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        userService.unlock(1L);

        assertThat(appUser.isAccountLocked()).isFalse();
    }

    @Test
    void _15_ShouldUpdateUsername_WhenNewUsernameAvailable() {
        UpdateProfileRequest request = new UpdateProfileRequest("newname", null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(appUserRepository.existsByUsername("newname")).thenReturn(false);
        when(appUserRepository.save(appUser)).thenReturn(appUser);
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        userService.updateProfile(1L, request);

        assertThat(appUser.getUsername()).isEqualTo("newname");
    }

    @Test
    void _16_ShouldThrowBusinessRule_WhenNewUsernameAlreadyUsed() {
        UpdateProfileRequest request = new UpdateProfileRequest("taken", null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(appUserRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in use");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void _17_ShouldUpdateEmail_WhenNewEmailAvailable() {
        UpdateProfileRequest request = new UpdateProfileRequest(null, "new@example.com");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(appUserRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(appUserRepository.save(appUser)).thenReturn(appUser);
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        userService.updateProfile(1L, request);

        assertThat(appUser.getEmail()).isEqualTo("new@example.com");
    }
}
