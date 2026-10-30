package com.edgareldy.springboottutorial.dto.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/roles}. Permission
 * assignment goes through the dedicated
 * {@code /api/v1/roles/{roleId}/permissions/{permissionId}} endpoints, not this
 * request.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record RoleRequest(

        @JsonProperty("role_name")
        @NotBlank(message = "Role name must not be blank")
        String roleName
) {

    public RoleRequest {
        roleName = roleName == null ? null : roleName.trim();
    }
}
