package com.mundial.apuesta.dto;

/** Resumen de las apuestas de un usuario para su perfil. */
public record EstadisticasApuestasResponse(
        long total,
        long puntuadas,
        long exactas,
        long ganadores,
        long diferencias,
        long fallos,
        long anticipadas
) {
}
