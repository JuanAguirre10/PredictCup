package com.mundial.pais;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaisRepository implements PanacheRepositoryBase<Pais, UUID> {

    public Optional<Pais> findByCodigoFifa(String codigo) {
        return find("codigoFifa", codigo).firstResultOptional();
    }

    public List<Pais> findByGrupo(String grupo) {
        return list("grupo", grupo);
    }
}
