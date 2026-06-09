package com.mundial.sala.dto;

import java.util.UUID;

/** Vista de una sala. */
public record SalaResponse(
        UUID id,
        String nombre,
        String codigo,
        String descripcion,
        boolean esPublica,
        int maxMiembros,
        UUID idDueno
) {
}
