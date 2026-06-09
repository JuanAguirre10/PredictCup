package com.mundial.apuesta.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Cuerpo de POST /api/apuestas. Solo campos que el cliente puede fijar.
 * La clave de idempotencia la genera el frontend (UUID) antes del POST.
 */
public record CrearApuestaRequest(
        @NotNull UUID idPartido,
        @NotNull @Min(0) Integer golesLocal,
        @NotNull @Min(0) Integer golesVisitante,
        @NotBlank @Size(max = 100) String claveIdempotencia
) {
}
