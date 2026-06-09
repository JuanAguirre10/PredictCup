package com.mundial.apuesta;

import com.mundial.partido.Partido;
import com.mundial.usuario.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "apuestas")
public class Apuesta extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    public Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_partido", nullable = false)
    public Partido partido;

    @Column(name = "id_usuario", insertable = false, updatable = false)
    public UUID idUsuario;

    @Column(name = "id_partido", insertable = false, updatable = false)
    public UUID idPartido;

    @Column(name = "goles_predichos_local", nullable = false)
    public int golesPredichosLocal;

    @Column(name = "goles_predichos_visit", nullable = false)
    public int golesPredichosVisit;

    @CreationTimestamp
    @Column(name = "registrado_en", nullable = false, updatable = false)
    public OffsetDateTime registradoEn;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 100)
    public String claveIdempotencia;

    @Column(name = "es_anticipada", nullable = false)
    public boolean esAnticipada = false;

    // Desglose de puntos: null hasta que el partido se puntua.
    @Column(name = "puntos_exacto")
    public Integer puntosExacto;

    @Column(name = "puntos_ganador")
    public Integer puntosGanador;

    @Column(name = "puntos_diferencia")
    public Integer puntosDiferencia;

    @Column(name = "bonus_anticipada")
    public Integer bonusAnticipada;

    @Column(name = "bonus_racha")
    public Integer bonusRacha;

    @Column(name = "puntos_ganados_total", nullable = false)
    public int puntosGanadosTotal = 0;

    @Column(name = "puntuado_en")
    public OffsetDateTime puntuadoEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    public OffsetDateTime creadoEn;
}
