package com.mundial.pais;

import com.mundial.partido.Partido;
import com.mundial.partido.TablaPosiciones;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "paises")
public class Pais extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "codigo_fifa", nullable = false, unique = true, length = 3)
    public String codigoFifa;

    @Column(nullable = false, length = 100)
    public String nombre;

    @Column(name = "nombre_es", nullable = false, length = 100)
    public String nombreEs;

    @Column(name = "bandera_emoji", length = 10)
    public String banderaEmoji;

    @Column(name = "bandera_url", length = 500)
    public String banderaUrl;

    @Column(nullable = false, length = 15)
    public String confederacion;

    @Column(length = 1)
    public String grupo;

    @Column(nullable = false)
    public boolean eliminado = false;

    @OneToMany(mappedBy = "paisLocal")
    public List<Partido> partidosLocal = new ArrayList<>();

    @OneToMany(mappedBy = "paisVisitante")
    public List<Partido> partidosVisitante = new ArrayList<>();

    @OneToOne(mappedBy = "pais")
    public TablaPosiciones tablaPosiciones;
}
