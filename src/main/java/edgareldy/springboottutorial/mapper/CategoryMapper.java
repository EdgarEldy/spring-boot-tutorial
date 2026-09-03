package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Category} and its DTOs. The
 * {@code id} and {@code products} fields are never set from a
 * {@link CategoryRequest}: {@code id} is database-generated, and
 * {@code products} is the reverse side of the relation, populated by
 * {@code Product}, not by a category write. Two response methods exist for
 * the same reason {@code OrderMapper} has two {@code toResponse} overloads:
 * {@link #toResponse(Category)} is used where {@code products} was never
 * fetched (the paginated list), {@link #toDetailResponse(Category)} where it
 * was (the single-category detail, backed by
 * {@code CategoryRepository.findByIdWithProducts}).
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface CategoryMapper {

    @Mapping(target = "products", expression = "java(java.util.List.of())")
    CategoryResponse toResponse(Category category);

    CategoryResponse toDetailResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CategoryRequest request, @MappingTarget Category category);
}
