package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for account activation token generation and single-use validation.
 * Not exposed by a controller directly: {@code AuthServiceImpl} calls
 * {@link #generate} after registration and {@link #validate} on the
 * activation endpoint.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface ActivationTokenService {

    String generate(AppUser user);

    AppUser validate(String token);
}
