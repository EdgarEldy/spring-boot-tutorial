package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.AuthService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link AuthService} implementation backed by
 * {@link AppUserRepository}, Spring Security's {@link AuthenticationManager}
 * for credential checking, and {@link JwtService} for token issuance.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ActivationTokenService activationTokenService;
    private final EmailService emailService;
    private final BlacklistedTokenService blacklistedTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username " + request.username() + " is already in use");
        }
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email " + request.email() + " is already in use");
        }
        Role userRole = roleRepository.findByRoleNameIgnoreCase("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Bootstrap role USER not found"));
        AppUser appUser = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .roles(Set.of(userRole))
                .build();
        AppUser saved = appUserRepository.save(appUser);
        String token = activationTokenService.generate(saved);
        emailService.sendActivationEmail(saved, token);
        return appUserMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // The client sends their email; Spring Security's AuthenticationManager
        // delegates to UserDetailsServiceImpl.loadUserByUsername() which looks
        // up accounts by username, not email. We resolve the username first so
        // the authenticate() call works without changing UserDetailsServiceImpl.
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), request.password()));
        String token = jwtService.generateToken(appUser);
        return new AuthResponse(token, BEARER_TOKEN_TYPE, appUser.getUsername());
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        String jti = jwtService.extractJti(rawToken);
        String username = jwtService.extractUsername(rawToken);
        Instant expiresAt = jwtService.extractExpiration(rawToken);
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + username));
        blacklistedTokenService.blacklist(user, rawToken, jti, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + username));
        return appUserMapper.toResponse(appUser);
    }
}
