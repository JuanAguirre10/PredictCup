package com.mundial.apuesta;

import com.mundial.apuesta.dto.ApuestaResponse;
import jakarta.enterprise.context.ApplicationScoped;

/** Conversion explicita Apuesta (entidad) -> DTO. Unico punto de mapeo. */
@ApplicationScoped
public class ApuestaMapper {

    public ApuestaResponse toResponse(Apuesta a) {
        return new ApuestaResponse(
                a.id,
                a.idPartido,
                a.golesPredichosLocal,
                a.golesPredichosVisit,
                a.esAnticipada,
                a.puntuadoEn == null ? null : a.puntosGanadosTotal,
                a.registradoEn,
                a.puntuadoEn);
    }
}
