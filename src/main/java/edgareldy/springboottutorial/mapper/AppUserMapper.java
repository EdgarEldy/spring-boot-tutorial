package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.auth.UserResponse;
import edgareldy.springboottutorial.entity.user.AppUser;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting {@link AppUser} to {@link UserResponse}. No
 * {@code toEntity} method here: building an {@code AppUser} requires
 * hashing the raw password and assigning a default {@code Role}, both of
 * which are business decisions made explicitly in {@code AuthServiceImpl}
 * rather than a generated field-by-field copy.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface AppUserMapper {

    UserResponse toResponse(AppUser appUser);
}
