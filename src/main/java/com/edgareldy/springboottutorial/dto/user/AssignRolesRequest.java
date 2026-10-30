package com.edgareldy.springboottutorial.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Request body for the PUT /users/{id}/roles endpoint. Replaces the user's
 * current role set with the provided list of role IDs.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record AssignRolesRequest(
        @NotNull(message = "Role IDs must not be null")
        @JsonProperty("role_ids") Set<Long> roleIds
) {
}
