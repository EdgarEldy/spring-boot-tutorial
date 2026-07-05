package edgareldy.springboottutorial.dto.category;

/**
 * Representation of a {@link edgareldy.springboottutorial.entity.Category}
 * returned by the API, never the JPA entity itself.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CategoryResponse(
        Long id,
        String categoryName
) {
}
