package com.mundial.puntuacion;

/**
 * Resultado inmutable del calculo de puntos de una apuesta.
 * No depende de Quarkus ni de JPA: es puro valor de dominio.
 */
public record ResultadoPuntuacion(
        int puntosBase,
        int bonusAnticipada,
        int bonusRacha,
        String tipoResultado
) {
    public int total() {
        return puntosBase + bonusAnticipada + bonusRacha;
    }

    public boolean acerto() {
        return puntosBase > 0;
    }
}
