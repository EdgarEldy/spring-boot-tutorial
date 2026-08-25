package edgareldy.springboottutorial.repository.specification;

import edgareldy.springboottutorial.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification} predicates for {@link Product}, combined
 * by {@code ProductServiceImpl} to build a dynamic filter for
 * {@code GET /api/v1/products} out of whichever optional query parameters
 * the caller actually supplied. Each method returns a {@code null}
 * predicate when its parameter is absent, which {@code Specification.and}
 * treats as "no restriction" rather than a match-nothing condition.
 * <p>
 * Created edgar.muhamyangabo on 8/25/26
 * Author : edgar.muhamyangabo
 * Date : 8/25/26
 * Project : spring-boot-tutorial
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, criteriaBuilder) -> categoryId == null
                ? null
                : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> nameContains(String productName) {
        return (root, query, criteriaBuilder) -> (productName == null || productName.isBlank())
                ? null
                : criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("productName")), "%" + productName.toLowerCase() + "%");
    }

    public static Specification<Product> priceBetween(Float minPrice, Float maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice != null && maxPrice != null) {
                return criteriaBuilder.between(root.get("unitPrice"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("unitPrice"), minPrice);
            }
            if (maxPrice != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("unitPrice"), maxPrice);
            }
            return null;
        };
    }
}
