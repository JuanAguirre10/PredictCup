package com.mundial.pais.dto;

import java.util.UUID;

/** Vista completa de un pais. */
public record PaisResponse(
        UUID id,
        String codigoFifa,
        String nombre,
        String nombreEs,
        String banderaEmoji,
        String banderaUrl,
        String confederacion,
        String grupo
) {
}
