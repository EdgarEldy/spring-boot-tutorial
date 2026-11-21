package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for account activation token generation and single-use validation.
 * Not exposed by a controller directly: {@code UserServiceImpl} calls
 * {@link #generate} after registration and {@link #validate} on the
 * activation endpoint. Tokens are 6-digit numeric OTPs (not UUID links)
 * so the user just types the code they received by email.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface ActivationTokenService {

    String generate(AppUser user);

    AppUser validate(String email, String otp);
}
