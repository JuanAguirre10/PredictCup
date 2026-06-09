package com.mundial.sala;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "miembros_sala")
public class MiembroSala extends PanacheEntityBase {

    @EmbeddedId
    public MiembroSalaId id;

    @CreationTimestamp
    @Column(name = "unido_en", nullable = false, updatable = false)
    public OffsetDateTime unidoEn;

    public MiembroSala() {
    }

    public MiembroSala(MiembroSalaId id) {
        this.id = id;
    }
}
