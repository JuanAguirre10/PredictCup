package com.mundial.notificaciones;

import com.mundial.apuesta.Apuesta;
import com.mundial.apuesta.ApuestaRepository;
import com.mundial.notificaciones.dto.NotificacionResponse;
import com.mundial.partido.Partido;
import com.mundial.usuario.UsuarioRepository;
import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Crea y entrega notificaciones. notificarPuntosCalculados se llama al cerrar un
 * partido; notificarPartidoPorEmpezar lo dispara un scanner programado ~30 min antes.
 */
@ApplicationScoped
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class);

    private final UsuarioRepository usuarioRepository;
    private final ApuestaRepository apuestaRepository;
    private final NotificacionMapper mapper;

    public NotificacionService(UsuarioRepository usuarioRepository,
                               ApuestaRepository apuestaRepository,
                               NotificacionMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.apuestaRepository = apuestaRepository;
        this.mapper = mapper;
    }

    /** Avisa a cada apostador cuantos puntos gano en el partido recien puntuado. */
    @Transactional
    public void notificarPuntosCalculados(UUID idPartido) {
        List<Apuesta> apuestas = apuestaRepository.findByPartido(idPartido);
        for (Apuesta a : apuestas) {
            Partido p = a.partido;
            String local = p.paisLocal != null ? p.paisLocal.nombreEs : "Local";
            String visitante = p.paisVisitante != null ? p.paisVisitante.nombreEs : "Visitante";

            Notificacion n = new Notificacion();
            n.idUsuario = a.idUsuario;
            n.tipo = "PUNTOS_CALCULADOS";
            n.titulo = "¡Puntos calculados!";
            n.cuerpo = "Ganaste " + a.puntosGanadosTotal + " pts en " + local + " vs " + visitante;
            n.idPartido = idPartido;
            n.persist();
        }
        LOG.infof("Notificadas %d apuestas de puntos (partido %s)", apuestas.size(), idPartido);
    }

    /** Avisa a cada apostador que su partido esta por empezar. */
    @Transactional
    public void notificarPartidoPorEmpezar(UUID idPartido) {
        List<Apuesta> apuestas = apuestaRepository.findByPartido(idPartido);
        for (Apuesta a : apuestas) {
            Partido p = a.partido;
            String local = p.paisLocal != null ? p.paisLocal.nombreEs : "Local";
            String visitante = p.paisVisitante != null ? p.paisVisitante.nombreEs : "Visitante";

            Notificacion n = new Notificacion();
            n.idUsuario = a.idUsuario;
            n.tipo = "PARTIDO_POR_EMPEZAR";
            n.titulo = "Tu partido esta por empezar";
            n.cuerpo = local + " vs " + visitante + " arranca pronto. ¡Mucha suerte!";
            n.idPartido = idPartido;
            n.persist();
        }
        LOG.infof("Notificadas %d apuestas de partido por empezar (%s)", apuestas.size(), idPartido);
    }

    /**
     * Cada 5 min busca partidos PROGRAMADO que arrancan en ~30 min y aun no fueron
     * avisados, y dispara la notificacion. La transaccion en este metodo permite que
     * las llamadas internas a notificarPartidoPorEmpezar persistan en la misma sesion.
     */
    @Scheduled(every = "5m", delayed = "1m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void escanearPartidosPorEmpezar() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime desde = ahora.plusMinutes(25);
        OffsetDateTime hasta = ahora.plusMinutes(35);
        List<Partido> proximos = Partido.list(
                "estado = ?1 and fechaHora between ?2 and ?3", "PROGRAMADO", desde, hasta);
        for (Partido p : proximos) {
            boolean yaAvisado = Notificacion.count(
                    "idPartido = ?1 and tipo = ?2", p.id, "PARTIDO_POR_EMPEZAR") > 0;
            if (!yaAvisado) {
                notificarPartidoPorEmpezar(p.id);
            }
        }
    }

    // ------------------------------------------------------ Lectura/estado

    public List<NotificacionResponse> noLeidas(UUID idUsuario) {
        return Notificacion.<Notificacion>find(
                        "idUsuario = ?1 and leida = false", Sort.descending("creadoEn"), idUsuario)
                .range(0, 19) // ultimas 20
                .list()
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public void marcarLeida(UUID idUsuario, UUID idNotificacion) {
        Notificacion n = Notificacion.findById(idNotificacion);
        if (n == null) {
            throw new NotFoundException("Notificacion no encontrada");
        }
        if (!n.idUsuario.equals(idUsuario)) {
            throw new ForbiddenException(); // IDOR: no es del usuario
        }
        n.leida = true;
        n.persist();
    }

    @Transactional
    public void marcarTodasLeidas(UUID idUsuario) {
        long n = Notificacion.update("leida = true where idUsuario = ?1 and leida = false", idUsuario);
        LOG.debugf("Marcadas %d notificaciones como leidas (usuario %s)", n, idUsuario);
    }
}
