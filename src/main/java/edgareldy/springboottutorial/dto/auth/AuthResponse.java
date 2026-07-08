package edgareldy.springboottutorial.dto.auth;

/**
 * Payload returned by {@code POST /api/v1/auth/login}: the JWT to send back
 * as a {@code Bearer} token on subsequent requests, plus enough profile
 * information for the client to avoid an extra call to {@code /auth/me}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record AuthResponse(
        String token,
        String tokenType,
        String username
) {
}
