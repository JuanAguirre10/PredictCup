package com.mundial.apuesta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ApuestaRepository implements PanacheRepositoryBase<Apuesta, UUID> {

    public Optional<Apuesta> findByClaveIdempotencia(String clave) {
        return find("claveIdempotencia", clave).firstResultOptional();
    }

    public Optional<Apuesta> findByUsuarioAndPartido(UUID idUsuario, UUID idPartido) {
        return find("idUsuario = ?1 and idPartido = ?2", idUsuario, idPartido).firstResultOptional();
    }

    public List<Apuesta> findByPartido(UUID idPartido) {
        return list("idPartido", idPartido);
    }

    public boolean existsByClaveIdempotencia(String clave) {
        return count("claveIdempotencia", clave) > 0;
    }

    public boolean existsByUsuarioAndPartido(UUID idUsuario, UUID idPartido) {
        return count("idUsuario = ?1 and idPartido = ?2", idUsuario, idPartido) > 0;
    }

    /**
     * Top apostadores de un grupo: suma de puntos ganados por usuario en apuestas
     * ya puntuadas de partidos de ese grupo. Devuelve filas
     * [idUsuario, nombreDisplay, urlAvatar, sumaPuntos, totalApuestas] ordenadas desc.
     */
    public List<Object[]> topApostadoresPorGrupo(String grupo, int limite) {
        return getEntityManager().createQuery(
                        "select a.usuario.id, a.usuario.nombreDisplay, a.usuario.urlAvatar, "
                                + "sum(a.puntosGanadosTotal), count(a) "
                                + "from Apuesta a "
                                + "where a.partido.grupo = :grupo and a.puntuadoEn is not null "
                                + "group by a.usuario.id, a.usuario.nombreDisplay, a.usuario.urlAvatar "
                                + "order by sum(a.puntosGanadosTotal) desc", Object[].class)
                .setParameter("grupo", grupo)
                .setMaxResults(limite)
                .getResultList();
    }
}
