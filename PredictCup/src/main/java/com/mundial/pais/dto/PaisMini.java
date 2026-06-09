package com.mundial.pais.dto;

import java.util.UUID;

/** Vista reducida de un pais para incrustar en partidos/posiciones. */
public record PaisMini(
        UUID id,
        String codigoFifa,
        String nombreEs,
        String banderaEmoji
) {
}
