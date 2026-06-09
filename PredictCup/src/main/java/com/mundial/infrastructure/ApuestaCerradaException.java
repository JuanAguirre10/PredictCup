package com.mundial.infrastructure;

/** Se intento apostar a un partido cuya ventana de apuestas ya cerro. -> HTTP 400. */
public class ApuestaCerradaException extends RuntimeException {
    public ApuestaCerradaException(String message) {
        super(message);
    }
}
