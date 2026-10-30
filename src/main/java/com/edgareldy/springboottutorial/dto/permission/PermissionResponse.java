package com.edgareldy.springboottutorial.dto.permission;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Permission}
 * returned by the API.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public record PermissionResponse(
        Long id,
        String resource,
        String action
) {
}
