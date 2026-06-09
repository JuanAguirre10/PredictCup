package com.mundial.partido.dto;

import com.mundial.estadio.dto.EstadioMini;
import com.mundial.pais.dto.PaisMini;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Vista completa del enfrentamiento: GET /api/partidos/{id}/vs. */
public record PartidoVsResponse(
        UUID id,
        PaisMini paisLocal,
        PaisMini paisVisitante,
        EstadioMini estadio,
        OffsetDateTime fechaHora,
        Integer golesLocal,
        Integer golesVisitante,
        String estado,
        String grupo,
        List<TablaPosicionResponse> tablaPosicionesGrupo
) {
}
