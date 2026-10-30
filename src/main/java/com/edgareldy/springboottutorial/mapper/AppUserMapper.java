package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting {@link AppUser} to {@link UserResponse}.
 * Delegates {@code Role} to {@code RoleResponse} mapping to {@link RoleMapper}.
 * No {@code toEntity} method here: building an {@code AppUser} requires
 * hashing the raw password and assigning a default {@code Role}, both of
 * which are business decisions made explicitly in {@code AuthServiceImpl}
 * rather than a generated field-by-field copy.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface AppUserMapper {

    UserResponse toResponse(AppUser appUser);
}
