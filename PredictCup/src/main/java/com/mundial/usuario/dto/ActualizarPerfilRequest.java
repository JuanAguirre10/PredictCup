package com.mundial.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de PATCH /api/usuarios/yo: datos editables del perfil. */
public record ActualizarPerfilRequest(
        @NotBlank @Size(max = 100) String nombreDisplay
) {
}
