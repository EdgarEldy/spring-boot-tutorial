package edgareldy.springboottutorial.dto.auth;

import edgareldy.springboottutorial.entity.user.Role;

/**
 * Representation of an {@link edgareldy.springboottutorial.entity.user.AppUser}
 * returned by the API, never the JPA entity itself and never the password.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
