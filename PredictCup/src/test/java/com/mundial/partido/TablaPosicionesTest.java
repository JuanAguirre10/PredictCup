package com.mundial.partido;

import com.mundial.apuesta.ApuestaService;
import com.mundial.infrastructure.FootballApiClient;
import com.mundial.notificaciones.NotificacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del recálculo de la tabla de posiciones (PartidoService) con
 * Mockito: se alimentan partidos TERMINADO y se verifica PJ/G/E/P/GF/GC/DG/Pts.
 */
@ExtendWith(MockitoExtension.class)
class TablaPosicionesTest {

    @Mock PartidoRepository partidoRepo;
    @Mock TablaPosicionesRepository tablaRepo;
    @Mock ApuestaService apuestaService;
    @Mock NotificacionService notificacionService;
    @Mock FootballApiClient footballApi;
    @Mock PartidoMapper mapper;

    @InjectMocks PartidoService service;

    /**
     * Monta un único partido TERMINADO {gl}-{gv} en el grupo C y ejecuta el
     * recálculo. Devuelve [posicion del local, posicion del visitante].
     */
    private TablaPosiciones[] calcular(int gl, int gv) {
        UUID partidoId = UUID.randomUUID();
        UUID local = UUID.randomUUID();
        UUID visitante = UUID.randomUUID();

        Partido p = new Partido();
        p.id = partidoId;
        p.grupo = "C";
        p.idPaisLocal = local;
        p.idPaisVisitante = visitante;
        p.golesLocal = gl;
        p.golesVisitante = gv;
        p.estado = "TERMINADO";

        TablaPosiciones tpLocal = new TablaPosiciones();
        tpLocal.grupo = "C";
        TablaPosiciones tpVisit = new TablaPosiciones();
        tpVisit.grupo = "C";

        when(partidoRepo.findById(partidoId)).thenReturn(p);
        when(partidoRepo.list(anyString(), eq("TERMINADO"), eq(local))).thenReturn(List.of(p));
        when(partidoRepo.list(anyString(), eq("TERMINADO"), eq(visitante))).thenReturn(List.of(p));
        when(tablaRepo.findByPaisAndGrupo(local, "C")).thenReturn(Optional.of(tpLocal));
        when(tablaRepo.findByPaisAndGrupo(visitante, "C")).thenReturn(Optional.of(tpVisit));
        when(tablaRepo.findByGrupoOrdenado("C")).thenReturn(List.of(tpLocal, tpVisit));

        service.actualizarTablaPosiciones(partidoId);
        return new TablaPosiciones[]{tpLocal, tpVisit};
    }

    @Test
    void victoriaLocal_sumaGanadoYTresPuntos() {
        TablaPosiciones local = calcular(2, 1)[0];
        assertEquals(1, local.partidosJugados);
        assertEquals(1, local.partidosGanados);
        assertEquals(3, local.puntos);
        assertEquals(2, local.golesFavor);
        assertEquals(1, local.golesContra);
        assertEquals(1, local.diferenciaGoles);
    }

    @Test
    void derrotaVisitante_sumaPerdidoSinPuntos() {
        TablaPosiciones visitante = calcular(2, 1)[1];
        assertEquals(1, visitante.partidosPerdidos);
        assertEquals(0, visitante.puntos);
        assertEquals(1, visitante.golesFavor);
        assertEquals(2, visitante.golesContra);
        assertEquals(-1, visitante.diferenciaGoles);
    }

    @Test
    void empate_sumaUnPuntoCadaUno() {
        TablaPosiciones[] r = calcular(1, 1);
        assertEquals(1, r[0].partidosEmpatados);
        assertEquals(1, r[0].puntos);
        assertEquals(1, r[1].partidosEmpatados);
        assertEquals(1, r[1].puntos);
        assertEquals(0, r[0].diferenciaGoles);
    }

    @Test
    void diferenciaDeGoles_correcta() {
        TablaPosiciones[] r = calcular(3, 0);
        assertEquals(3, r[0].diferenciaGoles);
        assertEquals(-3, r[1].diferenciaGoles);
    }
}
