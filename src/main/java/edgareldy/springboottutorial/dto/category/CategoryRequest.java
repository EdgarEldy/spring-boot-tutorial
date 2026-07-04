package edgareldy.springboottutorial.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/categories},
 * carrying only the fields a client is allowed to set.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CategoryRequest(

        @NotBlank(message = "categoryName must not be blank")
        String categoryName
) {
}
