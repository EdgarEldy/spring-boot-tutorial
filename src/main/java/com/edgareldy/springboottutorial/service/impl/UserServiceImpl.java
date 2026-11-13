package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.user.UpdateProfileRequest;
import com.edgareldy.springboottutorial.dto.user.UpdateUserRolesRequest;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.exception.TokenExpiredException;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import com.edgareldy.springboottutorial.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link UserService} implementation. Admin user management (list,
 * detail, role assignment, delete) and the three token-driven account flows:
 * account activation, forgot-password, and reset-password.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final AppUserMapper appUserMapper;
    private final ActivationTokenService activationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        Page<UserResponse> page = appUserRepository.findAll(pageable)
                .map(appUserMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return appUserMapper.toResponse(getUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long userId, UpdateUserRolesRequest request) {
        AppUser user = getUserOrThrow(userId);
        Set<Long> requestedIds = new HashSet<>(request.add());
        requestedIds.addAll(request.remove());
        List<Role> found = roleRepository.findAllById(requestedIds);
        if (found.size() != requestedIds.size()) {
            Set<Long> foundIds = found.stream().map(Role::getId).collect(Collectors.toSet());
            List<Long> missingIds = requestedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .sorted()
                    .toList();
            throw new ResourceNotFoundException("Roles not found with ids " + missingIds);
        }
        Set<Role> roles = user.getRoles();
        roles.removeIf(role -> request.remove().contains(role.getId()));
        found.stream()
                .filter(role -> request.add().contains(role.getId()))
                .forEach(roles::add);
        return appUserMapper.toResponse(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppUser user = getUserOrThrow(id);
        appUserRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse lock(Long id, String currentUsername) {
        AppUser user = getUserOrThrow(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessRuleException("Admin cannot lock their own account");
        }
        user.setAccountLocked(true);
        return appUserMapper.toResponse(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse unlock(Long id) {
        AppUser user = getUserOrThrow(id);
        user.setAccountLocked(false);
        return appUserMapper.toResponse(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        AppUser user = getUserOrThrow(id);
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (appUserRepository.existsByUsername(request.username())) {
                throw new BusinessRuleException("Username " + request.username() + " is already in use");
            }
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
                throw new BusinessRuleException("Email " + request.email() + " is already in use");
            }
            user.setEmail(request.email());
        }
        return appUserMapper.toResponse(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse activate(String token) {
        try {
            AppUser user = activationTokenService.validate(token);
            user.setEnabled(true);
            return appUserMapper.toResponse(appUserRepository.save(user));
        } catch (TokenExpiredException e) {
            // Token expired: generate a fresh one and re-send the email, then
            // rethrow so the caller still gets an error response (the user must
            // click the new link before they can log in).
            AppUser user = activationTokenService.getUserByToken(token);
            String newToken = activationTokenService.generate(user);
            emailService.sendActivationEmail(user, newToken);
            throw e;
        }
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        appUserRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEnabled() || user.isAccountLocked()) {
                return;
            }
            String token = passwordResetTokenService.generate(user);
            emailService.sendPasswordResetEmail(user, token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        AppUser user = passwordResetTokenService.validateAndConsume(token);
        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    private AppUser getUserOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
