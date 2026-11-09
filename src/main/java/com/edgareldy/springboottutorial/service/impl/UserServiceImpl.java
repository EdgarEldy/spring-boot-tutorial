package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.mapper.AppUserMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.service.ActivationTokenService;
import com.edgareldy.springboottutorial.service.EmailService;
import com.edgareldy.springboottutorial.service.PasswordResetTokenService;
import com.edgareldy.springboottutorial.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link UserService} implementation handling account activation and
 * password reset flows. Each flow delegates token creation/validation to the
 * dedicated token services and email sending to {@link EmailService}.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;
    private final ActivationTokenService activationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse activate(String token) {
        AppUser user = activationTokenService.validate(token);
        user.setEnabled(true);
        return appUserMapper.toResponse(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        appUserRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
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
}
