package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Category} and its DTOs.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    Category toEntity(CategoryRequest request);

    void updateEntityFromRequest(CategoryRequest request, @MappingTarget Category category);
}
