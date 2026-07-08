package edgareldy.springboottutorial.dto.order;

/**
 * Representation of a {@link edgareldy.springboottutorial.entity.Order}
 * returned by the API. Carries summarized {@code customer}/{@code product}
 * sub-objects rather than the full {@code CustomerResponse}/
 * {@code ProductResponse} shapes, since an order listing only needs enough
 * of each to identify them, not their full detail.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderResponse(
        Long id,
        CustomerSummary customer,
        ProductSummary product,
        int quantity,
        double total
) {

    public record CustomerSummary(Long id, String fullName) {
    }

    public record ProductSummary(Long id, String productName, float unitPrice) {
    }
}
