package com.mundial.partido;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TablaPosicionesRepository implements PanacheRepositoryBase<TablaPosiciones, UUID> {

    public List<TablaPosiciones> findByGrupoOrdenado(String grupo) {
        return list("grupo = ?1 order by puntos desc, diferenciaGoles desc, golesFavor desc", grupo);
    }

    public Optional<TablaPosiciones> findByPaisAndGrupo(UUID idPais, String grupo) {
        return find("idPais = ?1 and grupo = ?2", idPais, grupo).firstResultOptional();
    }
}
