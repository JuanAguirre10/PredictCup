package com.mundial.pais;

import com.mundial.pais.dto.PaisResponse;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** Consultas de paises. Solo lectura. */
@ApplicationScoped
public class PaisService {

    private final PaisRepository repository;
    private final PaisMapper mapper;

    public PaisService(PaisRepository repository, PaisMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<PaisResponse> listarTodos() {
        return repository.listAll(Sort.by("nombreEs"))
                .stream().map(mapper::toResponse).toList();
    }

    public List<PaisResponse> porGrupo(String grupo) {
        return repository.findByGrupo(grupo)
                .stream().map(mapper::toResponse).toList();
    }
}
