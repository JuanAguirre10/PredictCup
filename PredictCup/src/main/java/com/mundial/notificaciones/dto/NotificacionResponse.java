package com.mundial.notificaciones.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Vista de una notificacion. */
public record NotificacionResponse(
        UUID id,
        String tipo,
        String titulo,
        String cuerpo,
        UUID idPartido,
        UUID idSala,
        boolean leida,
        OffsetDateTime creadoEn
) {
}
