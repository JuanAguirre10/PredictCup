package com.mundial.infrastructure;

/** El cliente supero el limite de peticiones por minuto. -> HTTP 429. */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
