package edgareldy.springboottutorial.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a {@link edgareldy.springboottutorial.entity.Customer}
 * returned by the API, never the JPA entity itself. The JSON contract is
 * snake_case, decoupled from the Java fields' own camelCase names via
 * explicit {@link JsonProperty} annotations.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CustomerResponse(
        Long id,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        String telephone,
        String email,
        String address
) {
}
