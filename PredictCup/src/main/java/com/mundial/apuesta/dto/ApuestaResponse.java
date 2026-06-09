package com.mundial.apuesta.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Vista de una apuesta para el cliente. */
public record ApuestaResponse(
        UUID id,
        UUID idPartido,
        int golesLocal,
        int golesVisitante,
        boolean esAnticipada,
        Integer puntosTotal,
        OffsetDateTime registradoEn,
        OffsetDateTime puntuadoEn
) {
}
