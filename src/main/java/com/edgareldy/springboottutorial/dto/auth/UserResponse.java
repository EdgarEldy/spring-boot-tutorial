package com.edgareldy.springboottutorial.dto.auth;

import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Representation of an {@link com.edgareldy.springboottutorial.entity.user.AppUser}
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
        boolean enabled,
        @JsonProperty("account_locked") boolean accountLocked,
        @JsonProperty("roles") List<RoleResponse> roles
) {
}
