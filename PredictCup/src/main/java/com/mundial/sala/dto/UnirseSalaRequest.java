package com.mundial.sala.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/salas/unirse. */
public record UnirseSalaRequest(
        @NotBlank @Size(min = 6, max = 6) String codigo
) {
}
