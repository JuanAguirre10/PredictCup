package com.mundial.apuesta.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una apuesta del historial del usuario, enriquecida con el partido y el desglose de puntos. */
public record HistorialApuestaResponse(
        UUID id,
        UUID idPartido,
        String localNombre,
        String localEmoji,
        String localCodigoFifa,
        String visitanteNombre,
        String visitanteEmoji,
        String visitanteCodigoFifa,
        int golesLocal,
        int golesVisitante,
        Integer golesRealLocal,
        Integer golesRealVisitante,
        String estado,
        // EXACTO / GANADOR / DIFERENCIA / NINGUNO, o null si aún no se ha puntuado.
        String tipoResultado,
        Integer bonusAnticipada,
        Integer bonusRacha,
        Integer puntos,
        OffsetDateTime puntuadoEn
) {
}
