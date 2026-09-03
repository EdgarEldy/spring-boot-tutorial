package edgareldy.springboottutorial.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/categories},
 * carrying only the fields a client is allowed to set. The JSON contract is
 * snake_case ({@code category_name}), decoupled from the Java field's own
 * camelCase name via an explicit {@link JsonProperty}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CategoryRequest(

        @JsonProperty("category_name")
        @NotBlank(message = "category_name must not be blank")
        String categoryName
) {
}
