package com.mundial.sala;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MiembroSalaId implements Serializable {

    @Column(name = "id_sala")
    public UUID idSala;

    @Column(name = "id_usuario")
    public UUID idUsuario;

    public MiembroSalaId() {
    }

    public MiembroSalaId(UUID idSala, UUID idUsuario) {
        this.idSala = idSala;
        this.idUsuario = idUsuario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiembroSalaId that)) return false;
        return Objects.equals(idSala, that.idSala)
                && Objects.equals(idUsuario, that.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSala, idUsuario);
    }
}
