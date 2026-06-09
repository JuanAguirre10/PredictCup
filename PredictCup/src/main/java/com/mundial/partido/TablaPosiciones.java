package com.mundial.partido;

import com.mundial.pais.Pais;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tabla_posiciones")
public class TablaPosiciones extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais", nullable = false)
    public Pais pais;

    @Column(name = "id_pais", insertable = false, updatable = false)
    public UUID idPais;

    @Column(nullable = false, length = 1)
    public String grupo;

    @Column(nullable = false)
    public int posicion;

    @Column(name = "partidos_jugados", nullable = false)
    public int partidosJugados;

    @Column(name = "partidos_ganados", nullable = false)
    public int partidosGanados;

    @Column(name = "partidos_empatados", nullable = false)
    public int partidosEmpatados;

    @Column(name = "partidos_perdidos", nullable = false)
    public int partidosPerdidos;

    @Column(name = "goles_favor", nullable = false)
    public int golesFavor;

    @Column(name = "goles_contra", nullable = false)
    public int golesContra;

    @Column(name = "diferencia_goles", nullable = false)
    public int diferenciaGoles;

    @Column(nullable = false)
    public int puntos;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    public OffsetDateTime actualizadoEn;
}
