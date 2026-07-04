package edgareldy.springboottutorial.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/products}.
 * {@code categoryId} must reference an existing category; the service
 * layer, not this record, is responsible for checking that.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record ProductRequest(

        @NotNull(message = "categoryId must not be null")
        Long categoryId,

        @NotBlank(message = "productName must not be blank")
        String productName,

        @NotNull(message = "unitPrice must not be null")
        @Positive(message = "unitPrice must be greater than 0")
        Float unitPrice
) {
}
