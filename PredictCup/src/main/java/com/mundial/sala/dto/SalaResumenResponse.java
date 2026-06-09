package com.mundial.sala.dto;

import java.util.UUID;

/** Resumen de una sala para el listado "mis salas". */
public record SalaResumenResponse(
        UUID id,
        String nombre,
        String codigo,
        String descripcion,
        boolean esPublica,
        int miembros,
        boolean esDueno
) {
}
