package com.mundial.sala;

import com.mundial.sala.dto.SalaResponse;
import jakarta.enterprise.context.ApplicationScoped;

/** Conversion Sala (entidad) -> DTO. */
@ApplicationScoped
public class SalaMapper {

    public SalaResponse toResponse(Sala s) {
        return new SalaResponse(
                s.id, s.nombre, s.codigo, s.descripcion,
                s.esPublica, s.maxMiembros, s.idDueno);
    }
}
