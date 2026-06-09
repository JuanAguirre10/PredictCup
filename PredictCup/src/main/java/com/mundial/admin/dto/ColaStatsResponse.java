package com.mundial.admin.dto;

/** Métricas de la cola de apuestas para el panel admin. */
public record ColaStatsResponse(
        long pendientes,
        long procesadosHoy,
        long fallidos
) {
}
