package com.mundial.apuesta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro inmutable de como se puntuo una apuesta (auditoria). Una fila por
 * apuesta puntuada (UNIQUE id_apuesta). Se modela con columnas UUID planas:
 * no necesita navegar relaciones, solo dejar traza.
 */
@Entity
@Table(name = "historial_puntuacion")
public class HistorialPuntuacion extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "id_partido", nullable = false)
    public UUID idPartido;

    @Column(name = "id_usuario", nullable = false)
    public UUID idUsuario;

    @Column(name = "id_apuesta", nullable = false, unique = true)
    public UUID idApuesta;

    @Column(name = "puntos_base", nullable = false)
    public int puntosBase;

    @Column(name = "puntos_bonus", nullable = false)
    public int puntosBonus;

    @Column(name = "puntos_total", nullable = false)
    public int puntosTotal;

    @Column(name = "tipo_resultado", nullable = false, length = 15)
    public String tipoResultado = "ninguno";

    @Column(name = "tuvo_bonus_anticipada", nullable = false)
    public boolean tuvoBonusAnticipada;

    @Column(name = "tuvo_bonus_racha", nullable = false)
    public boolean tuvoBonusRacha;

    @CreationTimestamp
    @Column(name = "puntuado_en", nullable = false, updatable = false)
    public OffsetDateTime puntuadoEn;
}
