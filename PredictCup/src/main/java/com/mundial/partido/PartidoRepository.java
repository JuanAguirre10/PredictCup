package com.mundial.partido;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PartidoRepository implements PanacheRepositoryBase<Partido, UUID> {

    public List<Partido> findByFecha(LocalDate fecha) {
        OffsetDateTime desde = fecha.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime hasta = desde.plusDays(1);
        return list("fechaHora >= ?1 and fechaHora < ?2 order by fechaHora", desde, hasta);
    }

    public List<Partido> findByEstado(String estado) {
        return list("estado", estado);
    }

    public List<Partido> findByGrupo(String grupo) {
        return list("grupo = ?1 order by fechaHora", grupo);
    }

    public Optional<Partido> findByIdExterno(String idExterno) {
        return find("idExterno", idExterno).firstResultOptional();
    }

    public List<Partido> findProximosConApuestasAbiertas() {
        return list("estado = ?1 and cierreApuestas > ?2 order by fechaHora",
                "PROGRAMADO", OffsetDateTime.now(ZoneOffset.UTC));
    }
}
