package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.token.PasswordResetTokenType;
import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for password reset token generation and validation. Supports two
 * flows distinguished by {@link PasswordResetTokenType}:
 * <ul>
 *   <li>{@code LINK} - web flow: UUID token embedded in a reset link (1-hour TTL).
 *       Validated and consumed in one step by {@link #validateAndConsume}.</li>
 *   <li>{@code OTP} - mobile flow: 6-digit code typed by the user (5-minute TTL).
 *       The UI may first call {@link #verifyOtp} to confirm the code is valid
 *       before showing the new-password screen, then finalize via
 *       {@link #validateAndConsume}.</li>
 * </ul>
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface PasswordResetTokenService {

    String generate(AppUser user, PasswordResetTokenType type);

    AppUser validateAndConsume(String token);

    AppUser verifyOtp(String email, String otp);
}
