package edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
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
 * {@link AppUserMapper}, {@link PasswordEncoder}, {@link AuthenticationManager},
 * and {@link JwtService} mocked.
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
    private AppUserMapper appUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AppUser appUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed").role(Role.USER).build();
        userResponse = new UserResponse(1L, "ada", "ada@example.com", Role.USER);
    }

    @Test
    void registerSavesUserWithEncodedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        assertThat(authService.register(request)).isEqualTo(userResponse);

        ArgumentCaptor<AppUser> savedUserCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(savedUserCaptor.capture());
        assertThat(savedUserCaptor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(savedUserCaptor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerThrowsWhenUsernameAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("ada", "ada@example.com", "password123");
        when(appUserRepository.existsByUsername("ada")).thenReturn(false);
        when(appUserRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("ada", "password123");
        when(jwtService.generateToken("ada")).thenReturn("signed-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isEqualTo(new AuthResponse("signed-token", "Bearer", "ada"));
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("ada", "password123"));
    }

    @Test
    void loginThrowsWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("ada", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void meReturnsProfileWhenUserExists() {
        when(appUserRepository.findByUsername("ada")).thenReturn(Optional.of(appUser));
        when(appUserMapper.toResponse(appUser)).thenReturn(userResponse);

        assertThat(authService.me("ada")).isEqualTo(userResponse);
    }

    @Test
    void meThrowsWhenUserMissing() {
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
