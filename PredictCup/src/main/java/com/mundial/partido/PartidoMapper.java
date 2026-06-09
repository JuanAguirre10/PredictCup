package com.mundial.partido;

import com.mundial.apuesta.dto.ApuestaResponse;
import com.mundial.estadio.Estadio;
import com.mundial.estadio.dto.EstadioMini;
import com.mundial.pais.PaisMapper;
import com.mundial.partido.dto.PartidoResponse;
import com.mundial.partido.dto.PartidoVsResponse;
import com.mundial.partido.dto.TablaPosicionResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Conversion Partido/TablaPosiciones -> DTOs. Reutiliza PaisMapper para los
 * paises incrustados. Debe invocarse con la sesion de persistencia activa
 * (las relaciones paisLocal/paisVisitante/estadio son LAZY).
 */
@ApplicationScoped
public class PartidoMapper {

    private final PaisMapper paisMapper;

    public PartidoMapper(PaisMapper paisMapper) {
        this.paisMapper = paisMapper;
    }

    public PartidoResponse toResponse(Partido p, ApuestaResponse miApuesta) {
        return new PartidoResponse(
                p.id,
                p.idExterno,
                paisMapper.toMini(p.paisLocal),
                paisMapper.toMini(p.paisVisitante),
                toEstadioMini(p.estadio),
                p.fechaHora,
                p.cierreApuestas,
                p.fase,
                p.grupo,
                p.golesLocal,
                p.golesVisitante,
                p.estado,
                p.isApuestasAbiertas(),
                miApuesta);
    }

    public PartidoVsResponse toVs(Partido p, List<TablaPosicionResponse> tabla) {
        return new PartidoVsResponse(
                p.id,
                paisMapper.toMini(p.paisLocal),
                paisMapper.toMini(p.paisVisitante),
                toEstadioMini(p.estadio),
                p.fechaHora,
                p.golesLocal,
                p.golesVisitante,
                p.estado,
                p.grupo,
                tabla);
    }

    public TablaPosicionResponse toPosicion(TablaPosiciones t) {
        return new TablaPosicionResponse(
                t.posicion,
                paisMapper.toMini(t.pais),
                t.partidosJugados,
                t.partidosGanados,
                t.partidosEmpatados,
                t.partidosPerdidos,
                t.diferenciaGoles,
                t.puntos);
    }

    private EstadioMini toEstadioMini(Estadio e) {
        if (e == null) {
            return null;
        }
        return new EstadioMini(e.id, e.nombre, e.ciudad, e.paisSede);
    }
}
