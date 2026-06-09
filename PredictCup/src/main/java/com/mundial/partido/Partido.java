package com.mundial.partido;

import com.mundial.estadio.Estadio;
import com.mundial.pais.Pais;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "partidos")
public class Partido extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "id_externo", unique = true, length = 50)
    public String idExterno;

    // Nullable: en el bracket, un slot puede existir sin países asignados todavía.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais_local")
    public Pais paisLocal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais_visitante")
    public Pais paisVisitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estadio")
    public Estadio estadio;

    // Copias de solo lectura de las FK: permiten filtrar sin cargar la relacion.
    @Column(name = "id_pais_local", insertable = false, updatable = false)
    public UUID idPaisLocal;

    @Column(name = "id_pais_visitante", insertable = false, updatable = false)
    public UUID idPaisVisitante;

    @Column(name = "id_estadio", insertable = false, updatable = false)
    public UUID idEstadio;

    // Nullable: un slot del bracket puede no tener fecha hasta que el admin la asigne.
    @Column(name = "fecha_hora")
    public OffsetDateTime fechaHora;

    @Column(name = "cierre_apuestas")
    public OffsetDateTime cierreApuestas;

    @Column(nullable = false, length = 20)
    public String fase = "GRUPO";

    @Column(length = 1)
    public String grupo;

    public Integer jornada;

    // Orden del partido dentro de su ronda (posición en el bracket).
    public Integer orden;

    @Column(name = "goles_local")
    public Integer golesLocal;

    @Column(name = "goles_visitante")
    public Integer golesVisitante;

    @Column(name = "goles_local_et")
    public Integer golesLocalEt;

    @Column(name = "goles_visitante_et")
    public Integer golesVisitanteEt;

    @Column(name = "goles_local_pen")
    public Integer golesLocalPen;

    @Column(name = "goles_visitante_pen")
    public Integer golesVisitantePen;

    @Column(nullable = false, length = 15)
    public String estado = "PROGRAMADO";

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    public OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    public OffsetDateTime actualizadoEn;

    public boolean isApuestasAbiertas() {
        return "PROGRAMADO".equals(estado)
                && cierreApuestas != null
                && ZonedDateTime.now().isBefore(cierreApuestas.toInstant().atZone(ZoneOffset.UTC));
    }
}
