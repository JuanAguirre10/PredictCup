package com.mundial.sala;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SalaRepository implements PanacheRepositoryBase<Sala, UUID> {

    public Optional<Sala> findByCodigo(String codigo) {
        return find("codigo", codigo).firstResultOptional();
    }

    /** Salas de las que el usuario es miembro, más recientes primero. */
    public List<Sala> salasDeUsuario(UUID idUsuario) {
        return list("id in (select m.id.idSala from MiembroSala m where m.id.idUsuario = ?1) "
                + "order by creadoEn desc", idUsuario);
    }

    public boolean esMiembro(UUID idSala, UUID idUsuario) {
        return MiembroSala.count("id.idSala = ?1 and id.idUsuario = ?2", idSala, idUsuario) > 0;
    }

    public int contarMiembros(UUID idSala) {
        return (int) MiembroSala.count("id.idSala", idSala);
    }
}
