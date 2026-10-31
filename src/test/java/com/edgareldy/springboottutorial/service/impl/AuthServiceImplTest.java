package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.auth.AuthResponse;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link AuthServiceImpl}, with {@link AppUserRepository},
 * {@link RoleRepository}, {@link AppUserMapper}, {@link PasswordEncoder},
 * {@link AuthenticationManager}, {@link JwtService},
 * {@link ActivationTokenService}, {@link EmailService}, and
 * {@link BlacklistedTokenService} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;
    private AppUser appUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).roleName("USER").build();
        appUser = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed")
                .enabled(false).roles(Set.of(userRole)).build();
        userResponse = new UserResponse(1L, "ada", "ada@example.com", false, false,
                List.of(new RoleResponse(1L, "USER", List.of())));
    }

    @Test
    void _01_ShouldSaveUserDisabledAndSendActivationEmail_WhenRegistering() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(roleRepository.findByRoleNameIgnoreCase("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(activationTokenService.generate(appUser)).thenReturn("activation-token");
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        assertThat(authService.register(request)).isEqualTo(userResponse);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().isEnabled()).isFalse();
        verify(emailService).sendActivationEmail(eq(appUser), eq("activation-token"));
    }

    @Test
    void _02_ShouldThrowBusinessRule_WhenUsernameAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void _03_ShouldThrowBusinessRule_WhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void _04_ShouldReturnToken_WhenCredentialsValid() {
        LoginRequest request = new LoginRequest("ada@example.com", "password123");
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(appUser));
        when(jwtService.generateToken(appUser)).thenReturn("signed-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isEqualTo(new AuthResponse("signed-token", "Bearer", "ada"));
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("ada", "password123"));
    }

    @Test
    void _05_ShouldThrowAuthenticationException_WhenCredentialsInvalid() {
        LoginRequest request = new LoginRequest("ada@example.com", "wrong-password");
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any(AppUser.class));
    }

    @Test
    void _10_ShouldThrowBadCredentials_WhenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        when(appUserRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void _06_ShouldReturnProfile_WhenUserExists() {
        when(appUserRepository.findByUsername("ada")).thenReturn(Optional.of(appUser));
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        assertThat(authService.me("ada")).isEqualTo(userResponse);
    }

    @Test
    void _07_ShouldThrowNotFound_WhenUserMissing() {
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _08_ShouldBlacklistToken_WhenLogoutCalled() {
        String rawToken = "signed-token";
        String jti = "jti-123";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(jwtService.extractJti(rawToken)).thenReturn(jti);
        when(jwtService.extractUsername(rawToken)).thenReturn("ada");
        when(jwtService.extractExpiration(rawToken)).thenReturn(expiresAt);
        when(appUserRepository.findByUsername("ada")).thenReturn(Optional.of(appUser));

        authService.logout(rawToken);

        verify(blacklistedTokenService).blacklist(appUser, rawToken, jti, expiresAt);
    }

    @Test
    void _09_ShouldThrowNotFound_WhenLogoutCalledForUnknownUser() {
        String rawToken = "signed-token";
        when(jwtService.extractJti(rawToken)).thenReturn("jti-123");
        when(jwtService.extractUsername(rawToken)).thenReturn("ghost");
        when(jwtService.extractExpiration(rawToken)).thenReturn(Instant.now().plusSeconds(3600));
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(rawToken))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(blacklistedTokenService, never()).blacklist(any(), any(), any(), any());
    }
}
