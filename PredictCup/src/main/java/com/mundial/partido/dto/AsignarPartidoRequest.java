package com.mundial.partido.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cuerpo de PATCH /api/partidos/{id}/asignar (admin): completa un slot del bracket.
 * Todos los campos son opcionales (se puede asignar por partes: primero países, luego fecha).
 */
public record AsignarPartidoRequest(
        UUID idPaisLocal,
        UUID idPaisVisitante,
        UUID idEstadio,
        OffsetDateTime fechaHora
) {
}
