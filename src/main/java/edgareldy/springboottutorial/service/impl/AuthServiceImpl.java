package edgareldy.springboottutorial.service.impl;

import edgareldy.springboottutorial.dto.auth.AuthResponse;
import edgareldy.springboottutorial.dto.auth.LoginRequest;
import edgareldy.springboottutorial.dto.auth.RegisterRequest;
import edgareldy.springboottutorial.dto.auth.UserResponse;
import edgareldy.springboottutorial.entity.user.AppUser;
import edgareldy.springboottutorial.entity.user.Role;
import edgareldy.springboottutorial.exception.BusinessRuleException;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.mapper.AppUserMapper;
import edgareldy.springboottutorial.repository.AppUserRepository;
import edgareldy.springboottutorial.security.JwtService;
import edgareldy.springboottutorial.service.AuthService;
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

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username " + request.username() + " is already in use");
        }
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email " + request.email() + " is already in use");
        }
        AppUser appUser = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        return appUserMapper.toResponse(appUserRepository.save(appUser));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String token = jwtService.generateToken(request.username());
        return new AuthResponse(token, BEARER_TOKEN_TYPE, request.username());
    }

    @Override
    public UserResponse me(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + username));
        return appUserMapper.toResponse(appUser);
    }
}
