package com.mundial.notificaciones;

import com.mundial.notificaciones.dto.NotificacionResponse;
import jakarta.enterprise.context.ApplicationScoped;

/** Conversion Notificacion (entidad) -> DTO. */
@ApplicationScoped
public class NotificacionMapper {

    public NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(
                n.id, n.tipo, n.titulo, n.cuerpo,
                n.idPartido, n.idSala, n.leida, n.creadoEn);
    }
}
