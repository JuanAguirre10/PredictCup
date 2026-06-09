package com.mundial.usuario.dto;

import java.util.UUID;

/** Perfil publico del usuario autenticado. rol en minuscula ("usuario"/"admin"). */
public record UsuarioResponse(
        UUID id,
        String nombreDisplay,
        String correo,
        String urlAvatar,
        String rol,
        int puntosTotales,
        int rachaActual,
        boolean activo
) {
}
