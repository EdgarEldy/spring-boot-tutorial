package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.entity.user.Role;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.entity.token.RefreshToken;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.AuthService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.RefreshTokenService;
import java.time.Instant;
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
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final BlacklistedTokenService blacklistedTokenService;
    private final ActivationTokenService activationTokenService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        AppUser appUser = AppUser.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .enabled(false)
                .build();
        AppUser saved = appUserRepository.save(appUser);
        String token = activationTokenService.generate(saved);
        emailService.sendActivationEmail(saved, token);
        return appUserMapper.toResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // The client sends their email; Spring Security's AuthenticationManager
        // delegates to UserDetailsServiceImpl.loadUserByUsername() which looks
        // up accounts by username, not email. We resolve the username first so
        // the authenticate() call works without changing UserDetailsServiceImpl.
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), request.password()));
        String accessToken = jwtService.generateToken(appUser.getUsername());
        RefreshToken refreshToken = refreshTokenService.generate(appUser);
        return new AuthResponse(accessToken, BEARER_TOKEN_TYPE, appUser.getUsername(),
                refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.validate(rawRefreshToken);
        AppUser appUser = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(appUser.getUsername());
        RefreshToken newRefreshToken = refreshTokenService.generate(appUser);
        return new AuthResponse(newAccessToken, BEARER_TOKEN_TYPE, appUser.getUsername(),
                newRefreshToken.getToken());
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
        refreshTokenService.deleteByUser(user);
    }

    @Override
    public UserResponse me(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + username));
        return appUserMapper.toResponse(appUser);
    }
}
