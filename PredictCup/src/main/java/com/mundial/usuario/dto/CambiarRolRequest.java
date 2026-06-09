package com.mundial.usuario.dto;

import jakarta.validation.constraints.Pattern;

/** Cambio de rol (admin). */
public record CambiarRolRequest(
        @Pattern(regexp = "usuario|admin", message = "rol inválido") String rol
) {
}
