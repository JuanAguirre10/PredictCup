package com.mundial.puntuacion;

/**
 * Logica pura de puntuacion de apuestas. Sin anotaciones de Quarkus/CDI
 * para poder probarla como una clase normal (new ScoringService()).
 *
 * Reglas (en cascada, se aplica solo una categoria base):
 *   5 pts  EXACTO     - marcador identico.
 *   2 pts  DIFERENCIA - mismo ganador (no empate) y misma diferencia de goles,
 *                       pero marcador distinto. Esta regla tiene prioridad sobre
 *                       la de ganador cuando ambas se cumplen.
 *   3 pts  GANADOR    - mismo signo del resultado (incluye empate) sin ser exacto.
 *   0 pts  NINGUNO    - ningun acierto.
 *
 * Bonos (solo si puntosBase > 0):
 *   +2 racha       - rachaActual >= 2 (esta apuesta seria el 3er acierto seguido).
 *   +1 anticipada  - apuesta registrada de forma anticipada.
 */
public class ScoringService {

    public ResultadoPuntuacion calcular(
            int predLocal, int predVisitante,
            int realLocal, int realVisitante,
            int rachaActual, boolean esAnticipada) {

        int signoPred = signo(predLocal, predVisitante);
        int signoReal = signo(realLocal, realVisitante);

        int puntosBase;
        String tipoResultado;

        boolean exacto = predLocal == realLocal && predVisitante == realVisitante;
        boolean mismaDiferencia =
                Math.abs(predLocal - predVisitante) == Math.abs(realLocal - realVisitante);

        if (exacto) {
            puntosBase = 5;
            tipoResultado = "EXACTO";
        } else if (signoPred == signoReal && signoPred != 0 && mismaDiferencia) {
            // Mismo ganador y misma diferencia de goles, pero marcador distinto.
            puntosBase = 2;
            tipoResultado = "DIFERENCIA";
        } else if (signoPred == signoReal) {
            // Mismo signo (mismo ganador con diferencia distinta, o empate acertado).
            puntosBase = 3;
            tipoResultado = "GANADOR";
        } else {
            puntosBase = 0;
            tipoResultado = "NINGUNO";
        }

        boolean acerto = puntosBase > 0;
        int bonusRacha = (acerto && rachaActual >= 2) ? 2 : 0;
        int bonusAnticipada = (acerto && esAnticipada) ? 1 : 0;

        return new ResultadoPuntuacion(puntosBase, bonusAnticipada, bonusRacha, tipoResultado);
    }

    private int signo(int local, int visitante) {
        if (local > visitante) {
            return 1;
        }
        if (local < visitante) {
            return -1;
        }
        return 0;
    }
}
