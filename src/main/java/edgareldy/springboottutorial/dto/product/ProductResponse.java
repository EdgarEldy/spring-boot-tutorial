package edgareldy.springboottutorial.dto.product;

/**
 * Representation of a {@link edgareldy.springboottutorial.entity.Product}
 * returned by the API. Carries the parent category as a flat
 * {@code categoryId}/{@code categoryName} pair instead of a nested
 * {@code CategoryResponse}, matching the response shape documented in the
 * README.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record ProductResponse(
        Long id,
        String productName,
        float unitPrice,
        Long categoryId,
        String categoryName
) {
}
