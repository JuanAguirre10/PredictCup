package com.mundial.estadio.dto;

import java.util.UUID;

/** Vista reducida de un estadio para incrustar en partidos. */
public record EstadioMini(
        UUID id,
        String nombre,
        String ciudad,
        String paisSede
) {
}
