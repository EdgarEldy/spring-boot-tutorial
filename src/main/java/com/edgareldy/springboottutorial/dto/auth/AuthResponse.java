package com.edgareldy.springboottutorial.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned by {@code POST /api/v1/auth/login} and
 * {@code POST /api/v1/auth/refresh-token}. Fields follow the OAuth 2.0
 * naming convention: {@code access_token}, {@code token_type},
 * {@code refresh_token}, and {@code expires_in} (access token lifetime in
 * seconds). The client uses {@code expires_in} to schedule a proactive
 * refresh before the access token expires, avoiding a failed request.
 * {@code username} is omitted since it is already the {@code sub} claim
 * inside the JWT itself.
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
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn
) {

    // Overrides the compiler-generated toString(): LoggingAspect logs every
    // service method's return value via toString(), and login() returns
    // this record, so the default record toString() would put the raw JWT,
    // a live bearer credential, in the application logs.
    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=%s, refreshToken=***, expiresIn=%d]"
                .formatted(tokenType, expiresIn);
    }
}
