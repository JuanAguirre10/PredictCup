package com.mundial.apuesta;

import java.time.Instant;
import java.util.UUID;

/**
 * Carga serializada a JSON que viaja por la cola Redis entre el productor
 * (encolar, devuelve 202) y el worker (persistir). registradoEn se fija al
 * encolar para poder calcular el bonus de apuesta anticipada al persistir.
 */
public record ApuestaQueueItem(
        UUID idUsuario,
        UUID idPartido,
        int golesLocal,
        int golesVisitante,
        String claveIdempotencia,
        Instant registradoEn
) {
}
