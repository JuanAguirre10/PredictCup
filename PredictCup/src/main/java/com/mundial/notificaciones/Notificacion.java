package com.mundial.notificaciones;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Notificacion para un usuario (puntos calculados, partido por empezar, etc.).
 * Columnas UUID planas hacia usuario/partido/sala: solo necesita referenciar, no navegar.
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "id_usuario", nullable = false)
    public UUID idUsuario;

    @Column(nullable = false, length = 30)
    public String tipo;

    @Column(nullable = false, length = 150)
    public String titulo;

    @Column(nullable = false, length = 500)
    public String cuerpo;

    @Column(name = "id_partido")
    public UUID idPartido;

    @Column(name = "id_sala")
    public UUID idSala;

    @Column(nullable = false)
    public boolean leida = false;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    public OffsetDateTime creadoEn;
}
