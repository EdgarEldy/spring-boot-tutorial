package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.permission.PermissionRequest;
import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import com.edgareldy.springboottutorial.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Permission} and its DTOs.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    @Mapping(target = "id", ignore = true)
    Permission toEntity(PermissionRequest request);
}
