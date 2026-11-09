package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned by {@code POST /api/v1/auth/login}. The JSON field is
 * {@code access_token} (via {@link JsonProperty}) to distinguish it clearly
 * from the refresh token added in a later iteration, and to follow the
 * OAuth 2.0 naming convention. The {@code username} field is omitted: it is
 * already embedded as the {@code sub} claim inside the JWT itself, so the
 * client can decode the token rather than carry a redundant field.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record AuthResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("refresh_token")
        String refreshToken
) {

    // Overrides the compiler-generated toString(): LoggingAspect logs every
    // service method's return value via toString(), and login() returns
    // this record, so the default record toString() would put the raw JWT,
    // a live bearer credential, in the application logs.
    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=%s, refreshToken=***]"
                .formatted(tokenType);
    }
}
