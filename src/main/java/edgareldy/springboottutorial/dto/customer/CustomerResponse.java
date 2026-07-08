package edgareldy.springboottutorial.dto.customer;

/**
 * Representation of a {@link edgareldy.springboottutorial.entity.Customer}
 * returned by the API, never the JPA entity itself.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String telephone,
        String email,
        String address
) {
}
