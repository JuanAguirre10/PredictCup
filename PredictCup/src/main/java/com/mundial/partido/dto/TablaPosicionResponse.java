package com.mundial.partido.dto;

import com.mundial.pais.dto.PaisMini;

/** Una fila de la tabla de posiciones de un grupo. */
public record TablaPosicionResponse(
        int posicion,
        PaisMini pais,
        int partidosJugados,
        int partidosGanados,
        int partidosEmpatados,
        int partidosPerdidos,
        int diferenciaGoles,
        int puntos
) {
}
