package com.mundial.puntuacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests puros de ScoringService (sin Quarkus, sin contenedor CDI).
 * Convencion de argumentos:
 *   calcular(predLocal, predVisitante, realLocal, realVisitante, rachaActual, esAnticipada)
 */
class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    @Test
    @DisplayName("Marcador exacto con victoria local -> 5 pts")
    void exactoLocalGana() {
        ResultadoPuntuacion r = scoring.calcular(2, 1, 2, 1, 0, false);
        assertEquals(5, r.puntosBase());
        assertEquals("EXACTO", r.tipoResultado());
        assertEquals(5, r.total());
        assertTrue(r.acerto());
    }

    @Test
    @DisplayName("Marcador exacto con victoria visitante -> 5 pts")
    void exactoVisitanteGana() {
        ResultadoPuntuacion r = scoring.calcular(0, 2, 0, 2, 0, false);
        assertEquals(5, r.puntosBase());
        assertEquals("EXACTO", r.tipoResultado());
        assertEquals(5, r.total());
    }

    @Test
    @DisplayName("Marcador exacto con empate -> 5 pts")
    void exactoEmpate() {
        ResultadoPuntuacion r = scoring.calcular(1, 1, 1, 1, 0, false);
        assertEquals(5, r.puntosBase());
        assertEquals("EXACTO", r.tipoResultado());
        assertEquals(5, r.total());
    }

    @Test
    @DisplayName("Ganador correcto con diferencia distinta -> 3 pts")
    void ganadorCorrectoSinExacto() {
        // local gana en ambos, pero margen 2 vs 1
        ResultadoPuntuacion r = scoring.calcular(2, 0, 1, 0, 0, false);
        assertEquals(3, r.puntosBase());
        assertEquals("GANADOR", r.tipoResultado());
        assertEquals(3, r.total());
    }

    @Test
    @DisplayName("Empate acertado con marcador distinto -> 3 pts")
    void empateCorrectoSinExacto() {
        ResultadoPuntuacion r = scoring.calcular(1, 1, 2, 2, 0, false);
        assertEquals(3, r.puntosBase());
        assertEquals("GANADOR", r.tipoResultado());
        assertEquals(3, r.total());
    }

    @Test
    @DisplayName("Misma diferencia y mismo ganador, marcador distinto -> 2 pts")
    void diferenciaCorrectaMismoGanador() {
        // local gana en ambos, margen 1 == 1, pero 2-1 vs 3-2
        ResultadoPuntuacion r = scoring.calcular(2, 1, 3, 2, 0, false);
        assertEquals(2, r.puntosBase());
        assertEquals("DIFERENCIA", r.tipoResultado());
        assertEquals(2, r.total());
    }

    @Test
    @DisplayName("Empate acertado con misma diferencia (0) y distinto marcador -> 3 pts (es ganador)")
    void diferenciaEmpateEsGanador() {
        // ambos empate: la diferencia es 0==0 pero por ser empate puntua como GANADOR (3)
        ResultadoPuntuacion r = scoring.calcular(0, 0, 1, 1, 0, false);
        assertEquals(3, r.puntosBase());
        assertEquals("GANADOR", r.tipoResultado());
    }

    @Test
    @DisplayName("Ningun acierto -> 0 pts")
    void ningunAcierto() {
        // pred local gana, real visitante gana
        ResultadoPuntuacion r = scoring.calcular(2, 0, 0, 2, 0, false);
        assertEquals(0, r.puntosBase());
        assertEquals("NINGUNO", r.tipoResultado());
        assertEquals(0, r.total());
        assertFalse(r.acerto());
    }

    @Test
    @DisplayName("Bonus anticipada con acierto -> suma +1")
    void bonusAnticipadaConAcierto() {
        ResultadoPuntuacion r = scoring.calcular(2, 1, 2, 1, 0, true);
        assertEquals(5, r.puntosBase());
        assertEquals(1, r.bonusAnticipada());
        assertEquals(0, r.bonusRacha());
        assertEquals(6, r.total());
    }

    @Test
    @DisplayName("Bonus anticipada sin acierto -> NO suma")
    void bonusAnticipadaSinAcierto() {
        ResultadoPuntuacion r = scoring.calcular(2, 0, 0, 2, 0, true);
        assertEquals(0, r.puntosBase());
        assertEquals(0, r.bonusAnticipada());
        assertEquals(0, r.total());
    }

    @Test
    @DisplayName("Bonus racha con rachaActual=2 y acierto -> suma +2")
    void bonusRachaConAcierto() {
        ResultadoPuntuacion r = scoring.calcular(2, 1, 2, 1, 2, false);
        assertEquals(5, r.puntosBase());
        assertEquals(2, r.bonusRacha());
        assertEquals(0, r.bonusAnticipada());
        assertEquals(7, r.total());
    }

    @Test
    @DisplayName("Bonus racha con rachaActual=1 y acierto -> NO suma (racha incompleta)")
    void bonusRachaIncompleta() {
        ResultadoPuntuacion r = scoring.calcular(2, 1, 2, 1, 1, false);
        assertEquals(5, r.puntosBase());
        assertEquals(0, r.bonusRacha());
        assertEquals(5, r.total());
    }

    @Test
    @DisplayName("Bonus racha sin acierto -> NO suma")
    void bonusRachaSinAcierto() {
        ResultadoPuntuacion r = scoring.calcular(2, 0, 0, 2, 5, false);
        assertEquals(0, r.puntosBase());
        assertEquals(0, r.bonusRacha());
        assertEquals(0, r.total());
    }

    @Test
    @DisplayName("Maximo posible: exacto + anticipada + racha -> 8 pts")
    void maximoPosible() {
        ResultadoPuntuacion r = scoring.calcular(2, 1, 2, 1, 2, true);
        assertEquals(5, r.puntosBase());
        assertEquals(1, r.bonusAnticipada());
        assertEquals(2, r.bonusRacha());
        assertEquals(8, r.total());
    }

    @Test
    @DisplayName("Exacto sin bonos -> 5 pts exactos")
    void exactoSinBonos() {
        ResultadoPuntuacion r = scoring.calcular(3, 2, 3, 2, 0, false);
        assertEquals(5, r.puntosBase());
        assertEquals(0, r.bonusAnticipada());
        assertEquals(0, r.bonusRacha());
        assertEquals(5, r.total());
    }
}
