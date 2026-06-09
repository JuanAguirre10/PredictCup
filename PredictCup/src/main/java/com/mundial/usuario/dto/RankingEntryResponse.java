package com.mundial.usuario.dto;

import java.util.UUID;

/** Una fila del ranking global enriquecida con datos del usuario. */
public record RankingEntryResponse(
        int posicion,
        UUID idUsuario,
        String nombre,
        String urlAvatar,
        int puntos,
        int racha,
        long apuestas
) {
}
