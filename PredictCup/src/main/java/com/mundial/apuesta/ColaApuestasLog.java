package com.mundial.apuesta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Auditoria del paso de una apuesta por la cola Redis: una fila por clave de
 * idempotencia, con el estado final (PROCESADO / FALLIDO) y el tiempo de proceso.
 */
@Entity
@Table(name = "cola_apuestas_log")
public class ColaApuestasLog extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 100)
    public String claveIdempotencia;

    @Column(name = "id_usuario", nullable = false)
    public UUID idUsuario;

    @Column(name = "id_partido", nullable = false)
    public UUID idPartido;

    @Column(name = "goles_local", nullable = false)
    public int golesLocal;

    @Column(name = "goles_visitante", nullable = false)
    public int golesVisitante;

    @CreationTimestamp
    @Column(name = "encolado_en", nullable = false, updatable = false)
    public OffsetDateTime encoladoEn;

    @Column(name = "procesado_en")
    public OffsetDateTime procesadoEn;

    @Column(name = "tiempo_proceso_ms")
    public Integer tiempoProcesoMs;

    @Column(nullable = false, length = 15)
    public String estado = "EN_COLA";

    @Column(name = "mensaje_error", length = 2000)
    public String mensajeError;
}
