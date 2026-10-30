package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for password reset token generation and single-use validation.
 * Not exposed by a controller directly: {@code AuthServiceImpl} orchestrates
 * the forgot-password/reset-password flow around these two methods.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface PasswordResetTokenService {

    String generate(AppUser user);

    AppUser validateAndConsume(String token);
}
