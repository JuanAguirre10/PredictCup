package com.mundial.partido.dto;

import com.mundial.apuesta.dto.ApuestaResponse;
import com.mundial.estadio.dto.EstadioMini;
import com.mundial.pais.dto.PaisMini;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Vista de un partido para el listado y el detalle. miApuesta es null si anonimo o sin apuesta. */
public record PartidoResponse(
        UUID id,
        String idExterno,
        PaisMini paisLocal,
        PaisMini paisVisitante,
        EstadioMini estadio,
        OffsetDateTime fechaHora,
        OffsetDateTime cierreApuestas,
        String fase,
        String grupo,
        Integer golesLocal,
        Integer golesVisitante,
        String estado,
        boolean apuestasAbiertas,
        ApuestaResponse miApuesta
) {
}
