package com.edgareldy.springboottutorial.dto.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST /api/v1/permissions}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record PermissionRequest(

        @JsonProperty("resource")
        @NotBlank(message = "Resource must not be blank")
        String resource,

        @JsonProperty("action")
        @NotBlank(message = "Action must not be blank")
        String action
) {

    public PermissionRequest {
        resource = resource == null ? null : resource.trim();
        action = action == null ? null : action.trim();
    }
}
