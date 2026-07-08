package edgareldy.springboottutorial.service;

import edgareldy.springboottutorial.dto.auth.AuthResponse;
import edgareldy.springboottutorial.dto.auth.LoginRequest;
import edgareldy.springboottutorial.dto.auth.RegisterRequest;
import edgareldy.springboottutorial.dto.auth.UserResponse;

/**
 * Contract for registration, login, and profile lookup. Controllers and
 * tests depend on this interface, never on its implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse me(String username);
}
