package com.mundial.partido;

import com.mundial.infrastructure.FootballApiClient;
import com.mundial.infrastructure.FootballApiClient.MarcadorExterno;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Sincroniza marcadores con football-data.org cada 2 minutos. Solo mira partidos
 * activos recientes (programados o en curso de las ultimas 3 horas) para no gastar
 * el rate limit de la API en partidos viejos o lejanos.
 */
@ApplicationScoped
public class PartidoSyncJob {

    private static final Logger LOG = Logger.getLogger(PartidoSyncJob.class);

    private final PartidoRepository partidoRepository;
    private final PartidoService partidoService;
    private final FootballApiClient footballApi;

    public PartidoSyncJob(PartidoRepository partidoRepository,
                          PartidoService partidoService,
                          FootballApiClient footballApi) {
        this.partidoRepository = partidoRepository;
        this.partidoService = partidoService;
        this.footballApi = footballApi;
    }

    @Scheduled(every = "{app.sync.interval}", delayed = "15s",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sincronizarMarcadores() {
        OffsetDateTime limite = OffsetDateTime.now(ZoneOffset.UTC).minusHours(3);
        List<Partido> activos = partidoRepository.list(
                "estado in ?1 and fechaHora > ?2",
                List.of("PROGRAMADO", "EN_CURSO"), limite);

        int actualizados = 0;
        for (Partido p : activos) {
            if (p.idExterno == null) {
                continue;
            }
            MarcadorExterno m = footballApi.getMarcador(p.idExterno).orElse(null);
            if (m == null || !esDiferente(p, m)) {
                continue;
            }
            int golesLocal = m.golesLocal() != null ? m.golesLocal()
                    : (p.golesLocal != null ? p.golesLocal : 0);
            int golesVisit = m.golesVisitante() != null ? m.golesVisitante()
                    : (p.golesVisitante != null ? p.golesVisitante : 0);
            partidoService.actualizarMarcador(p.id, golesLocal, golesVisit, m.estado());
            actualizados++;
        }
        if (actualizados > 0) {
            LOG.infof("Sincronizacion: %d partidos actualizados de %d activos", actualizados, activos.size());
        }
    }

    private boolean esDiferente(Partido p, MarcadorExterno m) {
        return !Objects.equals(p.estado, m.estado())
                || !Objects.equals(p.golesLocal, m.golesLocal())
                || !Objects.equals(p.golesVisitante, m.golesVisitante());
    }
}
