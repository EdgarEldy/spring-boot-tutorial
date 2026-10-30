package com.edgareldy.springboottutorial.dto.role;

import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Role}
 * returned by the API, including its currently assigned permissions. Permissions
 * are included on every endpoint that returns a {@code RoleResponse}: roles and
 * permissions are reference data with a small, effectively fixed cardinality,
 * so the extra fetch per role is not worth special-casing away.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record RoleResponse(
        Long id,
        @JsonProperty("role_name") String roleName,
        List<PermissionResponse> permissions
) {
}
