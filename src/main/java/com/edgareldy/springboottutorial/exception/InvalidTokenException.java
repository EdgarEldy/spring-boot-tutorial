package com.edgareldy.springboottutorial.exception;

/**
 * Thrown when a token (account activation, password reset, or blacklisted JWT)
 * is not found, has already been consumed, or has expired. Maps to HTTP 400
 * via {@link GlobalExceptionHandler#handleInvalidToken}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
