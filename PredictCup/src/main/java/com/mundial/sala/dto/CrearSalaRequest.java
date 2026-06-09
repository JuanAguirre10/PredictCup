package com.mundial.sala.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/salas. */
public record CrearSalaRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255) String descripcion,
        boolean esPublica,
        @Min(2) @Max(1000) Integer maxMiembros
) {
}
