package com.mundial.partido.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de PATCH /api/partidos/{id}/marcador (admin). */
public record ActualizarMarcadorRequest(
        @NotNull @Min(0) Integer golesLocal,
        @NotNull @Min(0) Integer golesVisitante,
        @NotBlank @Pattern(regexp = "PROGRAMADO|EN_CURSO|EN_JUEGO|TERMINADO|SUSPENDIDO",
                message = "estado invalido") String estado
) {
}
