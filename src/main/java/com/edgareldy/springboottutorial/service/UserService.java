package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;

/**
 * Contract for token-driven user operations: account activation, password
 * reset request, and password reset confirmation. Controllers and tests
 * depend on this interface, never on its implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
public interface UserService {

    UserResponse activate(String token);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
