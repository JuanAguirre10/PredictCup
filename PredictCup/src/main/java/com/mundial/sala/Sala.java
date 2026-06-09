package com.mundial.sala;

import com.mundial.usuario.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "salas")
public class Sala extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dueno", nullable = false)
    public Usuario dueno;

    @Column(name = "id_dueno", insertable = false, updatable = false)
    public UUID idDueno;

    @Column(nullable = false, length = 100)
    public String nombre;

    @Column(nullable = false, unique = true, length = 6)
    public String codigo;

    @Column(length = 255)
    public String descripcion;

    @Column(name = "es_publica", nullable = false)
    public boolean esPublica = false;

    @Column(name = "max_miembros", nullable = false)
    public int maxMiembros = 100;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    public OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    public OffsetDateTime actualizadoEn;
}
