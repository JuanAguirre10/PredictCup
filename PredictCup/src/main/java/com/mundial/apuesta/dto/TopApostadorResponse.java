package com.mundial.apuesta.dto;

import java.util.UUID;

/** Apostador con mas puntos acumulados en los partidos de un grupo. */
public record TopApostadorResponse(
        UUID idUsuario,
        String nombre,
        String urlAvatar,
        int puntos,
        int aciertos
) {
}
