package com.edgareldy.springboottutorial.exception;

/**
 * Thrown when a token is structurally valid but its validity window has
 * passed. Extends {@link InvalidTokenException} so the existing
 * {@code GlobalExceptionHandler.handleInvalidToken} handler returns HTTP 400,
 * while callers that need to distinguish expiry from other invalidity
 * (e.g., to auto-resend a new activation link) can catch this subtype.
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
public class TokenExpiredException extends InvalidTokenException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
