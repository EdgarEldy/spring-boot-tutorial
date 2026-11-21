package com.edgareldy.springboottutorial.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Generates cryptographically secure tokens for account activation and
 * password reset. Provides two generation strategies:
 * <ul>
 *   <li>{@link #generate()} - URL-safe Base64 string (32 random bytes),
 *       suitable for web reset links embedded in a query string.</li>
 *   <li>{@link #generateOtp(int)} - Numeric OTP of the requested length,
 *       suitable for mobile flows where the user types a short code.</li>
 * </ul>
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Component
public class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generates a numeric OTP of exactly {@code length} digits (0-9).
     * Uses {@link SecureRandom} so each digit is cryptographically random.
     */
    public String generateOtp(int length) {
        StringBuilder otp = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
