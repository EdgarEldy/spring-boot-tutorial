package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.entity.user.Role;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link UserServiceImpl} covering account activation and
 * password reset flows. All dependencies are mocked with Mockito.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
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

    private AppUser user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L).firstName("Ada").lastName("Lovelace")
                .username("ada").email("ada@example.com")
                .password("hashed").role(Role.USER).enabled(false)
                .build();
        userResponse = new UserResponse(1L, "Ada", "Lovelace", "ada", "ada@example.com", Role.USER);
    }

    @Test
    void _01_ShouldEnableUser_WhenActivationTokenValid() {
        when(activationTokenService.validate("tok")).thenReturn(user);
        when(appUserRepository.save(user)).thenReturn(user);
        when(appUserMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.activate("tok");

        assertThat(user.isEnabled()).isTrue();
        assertThat(result.username()).isEqualTo("ada");
        verify(appUserRepository).save(user);
    }

    @Test
    void _02_ShouldSendResetEmail_WhenEmailKnown() {
        when(appUserRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.generate(user)).thenReturn("reset-tok");

        userService.forgotPassword("ada@example.com");

        verify(emailService).sendPasswordResetEmail(eq(user), eq("reset-tok"));
    }

    @Test
    void _03_ShouldNotSendEmail_WhenEmailUnknown() {
        when(appUserRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        userService.forgotPassword("unknown@example.com");

        verify(emailService, org.mockito.Mockito.never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void _04_ShouldEncodeAndSavePassword_WhenResetTokenValid() {
        when(passwordResetTokenService.validateAndConsume("reset-tok")).thenReturn(user);
        when(passwordEncoder.encode("NewPass1@")).thenReturn("hashed-new");

        userService.resetPassword("reset-tok", "NewPass1@");

        assertThat(user.getPassword()).isEqualTo("hashed-new");
        verify(appUserRepository).save(user);
    }
}
