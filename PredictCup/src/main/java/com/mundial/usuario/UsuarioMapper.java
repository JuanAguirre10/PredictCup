package com.mundial.usuario;

import com.mundial.usuario.dto.UsuarioResponse;
import jakarta.enterprise.context.ApplicationScoped;

/** Conversion Usuario (entidad) -> DTO. El rol se expone en minuscula. */
@ApplicationScoped
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.id,
                u.nombreDisplay,
                u.correo,
                u.urlAvatar,
                u.rol.name().toLowerCase(),
                u.puntosTotales,
                u.rachaActual,
                u.activo);
    }
}
