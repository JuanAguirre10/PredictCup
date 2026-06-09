package com.mundial.usuario;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario extends PanacheEntityBase {

    public enum Rol {
        USUARIO, ADMIN
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    public String googleSub;

    @Column(nullable = false, unique = true, length = 255)
    public String correo;

    @Column(name = "nombre_display", nullable = false, length = 100)
    public String nombreDisplay;

    @Column(name = "url_avatar", length = 500)
    public String urlAvatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Rol rol = Rol.USUARIO;

    @Column(name = "puntos_totales", nullable = false)
    public int puntosTotales = 0;

    @Column(name = "racha_actual", nullable = false)
    public int rachaActual = 0;

    @Column(nullable = false)
    public boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    public OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    public OffsetDateTime actualizadoEn;
}
