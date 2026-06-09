package com.mundial.estadio;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "estadios")
public class Estadio extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(nullable = false, length = 150)
    public String nombre;

    @Column(nullable = false, length = 100)
    public String ciudad;

    @Column(name = "pais_sede", nullable = false, length = 100)
    public String paisSede;

    public Integer capacidad;

    @Column(name = "imagen_url", length = 500)
    public String imagenUrl;
}
