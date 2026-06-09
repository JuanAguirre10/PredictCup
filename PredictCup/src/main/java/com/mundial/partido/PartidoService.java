package com.mundial.partido;

import com.mundial.apuesta.ApuestaService;
import com.mundial.apuesta.dto.ApuestaResponse;
import com.mundial.apuesta.dto.TopApostadorResponse;
import com.mundial.infrastructure.FootballApiClient;
import com.mundial.notificaciones.NotificacionService;
import com.mundial.pais.Pais;
import com.mundial.partido.dto.PartidoResponse;
import com.mundial.partido.dto.PartidoVsResponse;
import com.mundial.partido.dto.TablaPosicionResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reglas de partidos: listado/detalle (enriquecido con la apuesta del usuario),
 * actualizacion de marcador por admin, y recalculo de la tabla de posiciones.
 * Llama a ApuestaService (service-to-service), nunca a su repositorio.
 */
@ApplicationScoped
public class PartidoService {

    private static final Logger LOG = Logger.getLogger(PartidoService.class);

    private final PartidoRepository partidoRepository;
    private final TablaPosicionesRepository tablaRepository;
    private final ApuestaService apuestaService;
    private final NotificacionService notificacionService;
    private final FootballApiClient footballApi;
    private final PartidoMapper mapper;

    public PartidoService(PartidoRepository partidoRepository,
                          TablaPosicionesRepository tablaRepository,
                          ApuestaService apuestaService,
                          NotificacionService notificacionService,
                          FootballApiClient footballApi,
                          PartidoMapper mapper) {
        this.partidoRepository = partidoRepository;
        this.tablaRepository = tablaRepository;
        this.apuestaService = apuestaService;
        this.notificacionService = notificacionService;
        this.footballApi = footballApi;
        this.mapper = mapper;
    }

    /** Entidades crudas por fecha (uso interno). */
    public List<Partido> buscarPorFecha(LocalDate fecha) {
        return partidoRepository.findByFecha(fecha);
    }

    // ----------------------------------------------------------- Lecturas

    @Transactional
    public List<PartidoResponse> listarPorFecha(LocalDate fecha, UUID idUsuario) {
        return partidoRepository.findByFecha(fecha).stream()
                .map(p -> mapper.toResponse(p, miApuesta(idUsuario, p.id)))
                .toList();
    }

    @Transactional
    public PartidoResponse detalle(UUID id, UUID idUsuario) {
        Partido p = partidoRepository.findById(id);
        if (p == null) {
            throw new NotFoundException("Partido no encontrado: " + id);
        }
        return mapper.toResponse(p, miApuesta(idUsuario, p.id));
    }

    @Transactional
    public PartidoVsResponse vistaEnfrentamiento(UUID id) {
        Partido p = partidoRepository.findById(id);
        if (p == null) {
            throw new NotFoundException("Partido no encontrado: " + id);
        }
        List<TablaPosicionResponse> tabla =
                p.grupo == null ? List.of() : posicionesPorGrupo(p.grupo);
        return mapper.toVs(p, tabla);
    }

    @Transactional
    public List<TablaPosicionResponse> posicionesPorGrupo(String grupo) {
        return tablaRepository.findByGrupoOrdenado(grupo).stream()
                .map(mapper::toPosicion)
                .toList();
    }

    /** Partidos de un grupo (con resultado), enriquecidos con la apuesta del usuario si aplica. */
    @Transactional
    public List<PartidoResponse> listarPorGrupo(String grupo, UUID idUsuario) {
        return partidoRepository.findByGrupo(grupo).stream()
                .map(p -> mapper.toResponse(p, miApuesta(idUsuario, p.id)))
                .toList();
    }

    /** Top 10 apostadores del grupo (delegado a ApuestaService). */
    public List<TopApostadorResponse> topApostadoresGrupo(String grupo) {
        return apuestaService.topApostadoresPorGrupo(grupo, 10);
    }

    private ApuestaResponse miApuesta(UUID idUsuario, UUID idPartido) {
        if (idUsuario == null) {
            return null;
        }
        return apuestaService.miApuestaEnPartidoOpcional(idUsuario, idPartido);
    }

    // -------------------------------------------------------- Bracket (eliminatorias)

    /** Rondas del bracket en orden (nombre de fase, cantidad de partidos). */
    private static final List<Object[]> RONDAS = List.of(
            new Object[]{"DIECISEISAVOS", 16}, new Object[]{"OCTAVOS", 8},
            new Object[]{"CUARTOS", 4}, new Object[]{"SEMIFINAL", 2},
            new Object[]{"TERCER_PUESTO", 1}, new Object[]{"FINAL", 1});

    /** Todos los partidos de eliminatoria (fase != GRUPO), ordenados por posición. */
    @Transactional
    public List<PartidoResponse> listarBracket(UUID idUsuario) {
        return partidoRepository.list("fase <> ?1 order by orden", "GRUPO").stream()
                .map(p -> mapper.toResponse(p, miApuesta(idUsuario, p.id)))
                .toList();
    }

    /** Crea los slots vacíos del bracket (idempotente: no duplica una ronda ya creada). */
    @Transactional
    public void generarBracket() {
        for (Object[] ronda : RONDAS) {
            String fase = (String) ronda[0];
            int cantidad = (int) ronda[1];
            if (partidoRepository.count("fase", fase) > 0) {
                continue;
            }
            for (int i = 1; i <= cantidad; i++) {
                Partido p = new Partido();
                p.fase = fase;
                p.orden = i;
                p.estado = "PROGRAMADO";
                p.idExterno = "ko:" + fase + ":" + i;
                partidoRepository.persist(p);
            }
        }
        LOG.info("Bracket de eliminatorias generado (slots vacíos)");
    }

    /** Completa un slot del bracket: países, estadio y/o fecha (recalcula el cierre −10 min). */
    @Transactional
    public PartidoResponse asignarPartido(UUID id, UUID idLocal, UUID idVisit,
                                          UUID idEstadio, OffsetDateTime fechaHora) {
        Partido p = partidoRepository.findById(id);
        if (p == null) {
            throw new NotFoundException("Partido no encontrado: " + id);
        }
        var em = partidoRepository.getEntityManager();
        if (idLocal != null) {
            p.paisLocal = em.find(com.mundial.pais.Pais.class, idLocal);
        }
        if (idVisit != null) {
            p.paisVisitante = em.find(com.mundial.pais.Pais.class, idVisit);
        }
        if (idEstadio != null) {
            p.estadio = em.find(com.mundial.estadio.Estadio.class, idEstadio);
        }
        if (fechaHora != null) {
            p.fechaHora = fechaHora;
            p.cierreApuestas = fechaHora.minusMinutes(10);
        }
        LOG.infof("Slot %s (%s) asignado: local=%s visit=%s fecha=%s",
                id, p.fase, idLocal, idVisit, fechaHora);
        return mapper.toResponse(p, null);
    }

    // -------------------------------------------------------- Escrituras

    @Transactional
    public void actualizarMarcador(UUID id, int golesLocal, int golesVisit, String estado) {
        Partido partido = partidoRepository.findById(id);
        if (partido == null) {
            throw new NotFoundException("Partido no encontrado: " + id);
        }
        partido.golesLocal = golesLocal;
        partido.golesVisitante = golesVisit;
        partido.estado = estado;
        LOG.infof("Marcador actualizado partido=%s %d-%d estado=%s", id, golesLocal, golesVisit, estado);

        if ("TERMINADO".equals(estado)) {
            actualizarTablaPosiciones(id);
            apuestaService.puntuarPartido(id);
            // Notifica a los apostadores; el fan-out de ranking ya salio por Redis
            // Pub/Sub dentro de puntuarPartido().
            notificacionService.notificarPuntosCalculados(id);
            LOG.infof("Partido %s cerrado, puntuado y notificado", id);
        }
    }

    /**
     * Recalcula PJ/G/E/P/GF/GC/DG/Pts de los dos paises del partido a partir de
     * todos sus partidos TERMINADO, y reordena las posiciones (1..N) del grupo.
     */
    @Transactional
    public void actualizarTablaPosiciones(UUID idPartido) {
        Partido partido = partidoRepository.findById(idPartido);
        if (partido == null) {
            throw new NotFoundException("Partido no encontrado: " + idPartido);
        }
        String grupo = partido.grupo;
        if (grupo == null) {
            return; // fase eliminatoria: no hay tabla de grupo
        }
        recalcularPais(partido.idPaisLocal, grupo);
        recalcularPais(partido.idPaisVisitante, grupo);
        reordenarGrupo(grupo);
    }

    private void recalcularPais(UUID idPais, String grupo) {
        List<Partido> jugados = partidoRepository.list(
                "estado = ?1 and (idPaisLocal = ?2 or idPaisVisitante = ?2)", "TERMINADO", idPais);

        int pj = 0, g = 0, e = 0, p = 0, gf = 0, gc = 0;
        for (Partido m : jugados) {
            if (m.golesLocal == null || m.golesVisitante == null) {
                continue;
            }
            boolean esLocal = idPais.equals(m.idPaisLocal);
            int favor = esLocal ? m.golesLocal : m.golesVisitante;
            int contra = esLocal ? m.golesVisitante : m.golesLocal;
            pj++;
            gf += favor;
            gc += contra;
            if (favor > contra) {
                g++;
            } else if (favor == contra) {
                e++;
            } else {
                p++;
            }
        }

        TablaPosiciones tp = tablaRepository.findByPaisAndGrupo(idPais, grupo).orElseGet(() -> {
            TablaPosiciones nueva = new TablaPosiciones();
            nueva.pais = tablaRepository.getEntityManager().getReference(Pais.class, idPais);
            nueva.grupo = grupo;
            tablaRepository.persist(nueva);
            return nueva;
        });
        tp.partidosJugados = pj;
        tp.partidosGanados = g;
        tp.partidosEmpatados = e;
        tp.partidosPerdidos = p;
        tp.golesFavor = gf;
        tp.golesContra = gc;
        tp.diferenciaGoles = gf - gc;
        tp.puntos = g * 3 + e;
    }

    private void reordenarGrupo(String grupo) {
        List<TablaPosiciones> orden = tablaRepository.findByGrupoOrdenado(grupo);
        int posicion = 1;
        for (TablaPosiciones tp : orden) {
            tp.posicion = posicion++;
        }
    }
}
