package com.mundial.pais;

import com.mundial.pais.dto.PaisMini;
import com.mundial.pais.dto.PaisResponse;
import jakarta.enterprise.context.ApplicationScoped;

/** Conversion Pais (entidad) -> DTOs. */
@ApplicationScoped
public class PaisMapper {

    public PaisResponse toResponse(Pais p) {
        return new PaisResponse(
                p.id, p.codigoFifa, p.nombre, p.nombreEs,
                p.banderaEmoji, p.banderaUrl, p.confederacion, p.grupo);
    }

    public PaisMini toMini(Pais p) {
        if (p == null) {
            return null;
        }
        return new PaisMini(p.id, p.codigoFifa, p.nombreEs, p.banderaEmoji);
    }
}
