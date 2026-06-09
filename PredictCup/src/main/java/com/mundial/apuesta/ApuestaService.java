package com.mundial.apuesta;

import com.mundial.apuesta.dto.ApuestaResponse;
import com.mundial.apuesta.dto.EstadisticasApuestasResponse;
import com.mundial.apuesta.dto.HistorialApuestaResponse;
import com.mundial.apuesta.dto.TopApostadorResponse;
import com.mundial.infrastructure.ApuestaCerradaException;
import com.mundial.infrastructure.ConflictException;
import com.mundial.infrastructure.RedisService;
import com.mundial.partido.Partido;
import com.mundial.partido.PartidoRepository;
import com.mundial.puntuacion.ResultadoPuntuacion;
import com.mundial.puntuacion.ScoringService;
import com.mundial.sala.MiembroSala;
import com.mundial.usuario.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reglas de negocio de apuestas y puntuacion. Las apuestas nunca se escriben de
 * forma sincrona: se validan y se encolan (202); el worker las persiste en lote.
 * Refina mundial-quarkus-architect: @Transactional vive aqui, no en el resource.
 */
@ApplicationScoped
public class ApuestaService {

    private static final Logger LOG = Logger.getLogger(ApuestaService.class);

    private final ApuestaRepository apuestaRepository;
    private final PartidoRepository partidoRepository;
    private final RedisService redisService;
    private final ApuestaMapper mapper;
    private final ObjectMapper json;
    // Logica pura, sin CDI: se instancia con new (arch-pure-domain-no-cdi).
    private final ScoringService scoringService = new ScoringService();

    public ApuestaService(ApuestaRepository apuestaRepository,
                          PartidoRepository partidoRepository,
                          RedisService redisService,
                          ApuestaMapper mapper,
                          ObjectMapper json) {
        this.apuestaRepository = apuestaRepository;
        this.partidoRepository = partidoRepository;
        this.redisService = redisService;
        this.mapper = mapper;
        this.json = json;
    }

    /**
     * Valida y encola la apuesta. NO escribe en la BD: el cliente recibe 202.
     * El rate limit se aplica en RateLimitFilter (no se re-incrementa aqui).
     */
    public void encolarApuesta(UUID idUsuario, UUID idPartido,
                               int golesLocal, int golesVisitante, String claveIdempotencia) {
        Partido partido = partidoRepository.findById(idPartido);
        if (partido == null) {
            throw new NotFoundException("Partido no encontrado: " + idPartido);
        }
        if (!partido.isApuestasAbiertas()) {
            throw new ApuestaCerradaException("Las apuestas para este partido estan cerradas");
        }
        ApuestaQueueItem item = new ApuestaQueueItem(
                idUsuario, idPartido, golesLocal, golesVisitante, claveIdempotencia, Instant.now());
        try {
            redisService.encolarApuesta(json.writeValueAsString(item));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo serializar la apuesta para la cola", e);
        }
        LOG.infof("Apuesta encolada usuario=%s partido=%s clave=%s", idUsuario, idPartido, claveIdempotencia);
    }

    /** Persiste una apuesta tomada de la cola. Doble barrera de idempotencia + unicidad. */
    @Transactional
    public void persistirApuesta(ApuestaQueueItem item) {
        if (apuestaRepository.existsByClaveIdempotencia(item.claveIdempotencia())) {
            return; // reintento de la misma peticion
        }
        if (apuestaRepository.existsByUsuarioAndPartido(item.idUsuario(), item.idPartido())) {
            return; // el usuario ya aposto a este partido
        }
        Partido partido = partidoRepository.findById(item.idPartido());
        if (partido == null) {
            LOG.warnf("Descartando apuesta encolada: partido %s ya no existe", item.idPartido());
            return;
        }
        boolean esAnticipada = item.registradoEn()
                .isBefore(partido.fechaHora.toInstant().minus(Duration.ofHours(24)));

        Apuesta apuesta = new Apuesta();
        apuesta.usuario = apuestaRepository.getEntityManager().getReference(Usuario.class, item.idUsuario());
        apuesta.partido = partido;
        apuesta.golesPredichosLocal = item.golesLocal();
        apuesta.golesPredichosVisit = item.golesVisitante();
        apuesta.claveIdempotencia = item.claveIdempotencia();
        apuesta.esAnticipada = esAnticipada;
        // registrado_en lo fija @CreationTimestamp al persistir.
        apuestaRepository.persist(apuesta);
        LOG.debugf("Apuesta persistida %s", apuesta.id);
    }

    /** Puntua todas las apuestas de un partido ya TERMINADO y actualiza rankings. */
    @Transactional
    public void puntuarPartido(UUID idPartido) {
        Partido partido = partidoRepository.findById(idPartido);
        if (partido == null) {
            throw new NotFoundException("Partido no encontrado: " + idPartido);
        }
        if (!"TERMINADO".equals(partido.estado)) {
            throw new ConflictException("El partido no esta TERMINADO; no se puede puntuar");
        }
        if (partido.golesLocal == null || partido.golesVisitante == null) {
            throw new ConflictException("El partido TERMINADO no tiene marcador real");
        }

        List<Apuesta> apuestas = apuestaRepository.findByPartido(idPartido);
        for (Apuesta apuesta : apuestas) {
            if (apuesta.puntuadoEn != null) {
                continue; // ya puntuada (idempotencia del cierre)
            }
            Usuario usuario = apuesta.usuario;
            ResultadoPuntuacion r = scoringService.calcular(
                    apuesta.golesPredichosLocal, apuesta.golesPredichosVisit,
                    partido.golesLocal, partido.golesVisitante,
                    usuario.rachaActual, apuesta.esAnticipada);

            apuesta.puntosExacto = "EXACTO".equals(r.tipoResultado()) ? r.puntosBase() : 0;
            apuesta.puntosGanador = "GANADOR".equals(r.tipoResultado()) ? r.puntosBase() : 0;
            apuesta.puntosDiferencia = "DIFERENCIA".equals(r.tipoResultado()) ? r.puntosBase() : 0;
            apuesta.bonusAnticipada = r.bonusAnticipada();
            apuesta.bonusRacha = r.bonusRacha();
            apuesta.puntosGanadosTotal = r.total();
            apuesta.puntuadoEn = OffsetDateTime.now();

            usuario.puntosTotales += r.total();
            usuario.rachaActual = r.acerto() ? usuario.rachaActual + 1 : 0;

            redisService.actualizarRanking("global", usuario.id, r.total());
            for (MiembroSala m : MiembroSala.<MiembroSala>list("id.idUsuario", usuario.id)) {
                redisService.actualizarRanking("sala:" + m.id.idSala, usuario.id, r.total());
            }

            HistorialPuntuacion h = new HistorialPuntuacion();
            h.idPartido = idPartido;
            h.idUsuario = usuario.id;
            h.idApuesta = apuesta.id;
            h.puntosBase = r.puntosBase();
            h.puntosBonus = r.bonusAnticipada() + r.bonusRacha();
            h.puntosTotal = r.total();
            h.tipoResultado = r.tipoResultado();
            h.tuvoBonusAnticipada = r.bonusAnticipada() > 0;
            h.tuvoBonusRacha = r.bonusRacha() > 0;
            h.persist();
        }

        redisService.publicar("ranking:actualizado", idPartido.toString());
        LOG.infof("Partido %s puntuado: %d apuestas", idPartido, apuestas.size());
    }

    // ----------------------------------------------------------- Consultas

    public List<ApuestaResponse> misApuestas(UUID idUsuario) {
        return apuestaRepository.list("idUsuario", idUsuario)
                .stream().map(mapper::toResponse).toList();
    }

    /** Mi apuesta para un partido, o 404 si no existe. */
    public ApuestaResponse miApuestaEnPartido(UUID idUsuario, UUID idPartido) {
        return apuestaRepository.findByUsuarioAndPartido(idUsuario, idPartido)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No tienes apuesta para este partido"));
    }

    /** Igual que el anterior pero sin lanzar: para enriquecer otras vistas (null si no hay). */
    public ApuestaResponse miApuestaEnPartidoOpcional(UUID idUsuario, UUID idPartido) {
        return apuestaRepository.findByUsuarioAndPartido(idUsuario, idPartido)
                .map(mapper::toResponse)
                .orElse(null);
    }

    /** Conteo de apuestas por usuario (para enriquecer el ranking). */
    public Map<UUID, Long> contarPorUsuarios(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = apuestaRepository.getEntityManager().createQuery(
                        "select a.idUsuario, count(a) from Apuesta a where a.idUsuario in :ids "
                                + "group by a.idUsuario", Object[].class)
                .setParameter("ids", ids)
                .getResultList();
        Map<UUID, Long> conteos = new HashMap<>();
        for (Object[] r : rows) {
            conteos.put((UUID) r[0], ((Number) r[1]).longValue());
        }
        return conteos;
    }

    /** Cuántas apuestas hay para un partido (panel admin). */
    public long contarPorPartido(UUID idPartido) {
        return apuestaRepository.count("idPartido", idPartido);
    }

    /** Estadísticas agregadas de un usuario para su perfil. */
    public EstadisticasApuestasResponse estadisticasUsuario(UUID idUsuario) {
        List<Apuesta> as = apuestaRepository.list("idUsuario", idUsuario);
        long total = as.size();
        long puntuadas = as.stream().filter(a -> a.puntuadoEn != null).count();
        long exactas = as.stream().filter(a -> a.puntosExacto != null && a.puntosExacto > 0).count();
        long ganadores = as.stream().filter(a -> a.puntosGanador != null && a.puntosGanador > 0).count();
        long diferencias = as.stream().filter(a -> a.puntosDiferencia != null && a.puntosDiferencia > 0).count();
        long anticipadas = as.stream().filter(a -> a.esAnticipada).count();
        long fallos = as.stream().filter(a -> a.puntuadoEn != null && a.puntosGanadosTotal == 0).count();
        return new EstadisticasApuestasResponse(total, puntuadas, exactas, ganadores, diferencias, fallos, anticipadas);
    }

    /**
     * Historial de las últimas apuestas del usuario, con el partido enriquecido.
     * Un solo SELECT con JOIN FETCH (partido + ambos países) evita el N+1.
     */
    public List<HistorialApuestaResponse> historialUsuario(UUID idUsuario, int limite) {
        List<Apuesta> apuestas = apuestaRepository.getEntityManager().createQuery(
                        "select a from Apuesta a "
                                + "join fetch a.partido p "
                                + "join fetch p.paisLocal join fetch p.paisVisitante "
                                + "where a.idUsuario = :u order by a.registradoEn desc", Apuesta.class)
                .setParameter("u", idUsuario)
                .setMaxResults(limite)
                .getResultList();
        return apuestas.stream()
                .map(a -> {
                    Partido p = a.partido;
                    String tipo = null;
                    if (a.puntuadoEn != null) {
                        if (a.puntosExacto != null && a.puntosExacto > 0) {
                            tipo = "EXACTO";
                        } else if (a.puntosGanador != null && a.puntosGanador > 0) {
                            tipo = "GANADOR";
                        } else if (a.puntosDiferencia != null && a.puntosDiferencia > 0) {
                            tipo = "DIFERENCIA";
                        } else {
                            tipo = "NINGUNO";
                        }
                    }
                    return new HistorialApuestaResponse(
                            a.id, a.idPartido,
                            p.paisLocal.nombreEs, p.paisLocal.banderaEmoji, p.paisLocal.codigoFifa,
                            p.paisVisitante.nombreEs, p.paisVisitante.banderaEmoji, p.paisVisitante.codigoFifa,
                            a.golesPredichosLocal, a.golesPredichosVisit,
                            p.golesLocal, p.golesVisitante, p.estado,
                            tipo,
                            a.puntuadoEn != null ? a.bonusAnticipada : null,
                            a.puntuadoEn != null ? a.bonusRacha : null,
                            a.puntuadoEn != null ? a.puntosGanadosTotal : null,
                            a.puntuadoEn);
                })
                .toList();
    }

    /** Top apostadores (mas puntos acumulados) en los partidos de un grupo. */
    public List<TopApostadorResponse> topApostadoresPorGrupo(String grupo, int limite) {
        return apuestaRepository.topApostadoresPorGrupo(grupo, limite).stream()
                .map(r -> new TopApostadorResponse(
                        (UUID) r[0],
                        (String) r[1],
                        (String) r[2],
                        ((Number) r[3]).intValue(),
                        ((Number) r[4]).intValue()))
                .toList();
    }

    /**
     * Registra/actualiza la auditoria del paso por la cola (PROCESADO/FALLIDO).
     * Transaccion propia: el worker la invoca por item, independiente del persist.
     */
    @Transactional
    public void registrarProcesoCola(ApuestaQueueItem item, String estado, long tiempoMs, String error) {
        ColaApuestasLog log = ColaApuestasLog.<ColaApuestasLog>find("claveIdempotencia", item.claveIdempotencia())
                .firstResultOptional()
                .orElseGet(ColaApuestasLog::new);
        if (log.id == null) {
            log.claveIdempotencia = item.claveIdempotencia();
            log.idUsuario = item.idUsuario();
            log.idPartido = item.idPartido();
            log.golesLocal = item.golesLocal();
            log.golesVisitante = item.golesVisitante();
        }
        log.estado = estado;
        log.procesadoEn = OffsetDateTime.now();
        log.tiempoProcesoMs = (int) tiempoMs;
        log.mensajeError = error;
        log.persist();
    }
}
