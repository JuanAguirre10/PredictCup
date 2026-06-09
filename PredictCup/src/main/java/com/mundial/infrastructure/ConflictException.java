package com.mundial.infrastructure;

/** Conflicto de estado (p.ej. recurso duplicado). -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
