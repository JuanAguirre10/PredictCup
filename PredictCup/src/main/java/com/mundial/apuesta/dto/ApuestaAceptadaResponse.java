package com.mundial.apuesta.dto;

/** Respuesta 202: la apuesta entro a la cola, aun no esta persistida. */
public record ApuestaAceptadaResponse(
        String claveIdempotencia,
        String estado
) {
    public static ApuestaAceptadaResponse aceptada(String claveIdempotencia) {
        return new ApuestaAceptadaResponse(claveIdempotencia, "ACEPTADA");
    }
}
