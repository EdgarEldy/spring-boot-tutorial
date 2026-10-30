package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.role.RoleRequest;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Role} and its DTOs, delegating
 * permission mapping to {@link PermissionMapper}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = PermissionMapper.class)
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleRequest request);
}
