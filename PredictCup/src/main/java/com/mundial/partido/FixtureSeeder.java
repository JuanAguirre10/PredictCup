package com.mundial.partido;

import com.mundial.estadio.Estadio;
import com.mundial.pais.Pais;
import com.mundial.pais.PaisRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Siembra el fixture de demo (round-robin de los 12 grupos = 72 partidos) al arrancar,
 * solo si app.demo.seed-fixtures=true y aun no hay partidos. Los kickoffs se escalonan en
 * tiempo real y cada partido lleva id_externo "demo:&lt;kickoff&gt;:&lt;semilla&gt;" para que el
 * simulador del FootballApiClient lo "juegue".
 */
@ApplicationScoped
public class FixtureSeeder {

    private static final Logger LOG = Logger.getLogger(FixtureSeeder.class);
    private static final String[] GRUPOS =
            {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
    // Round-robin de 4 equipos en 3 jornadas (indices dentro del grupo).
    private static final int[][] PARES = {{0, 1}, {2, 3}, {0, 2}, {1, 3}, {0, 3}, {1, 2}};

    private final PartidoRepository partidoRepository;
    private final PaisRepository paisRepository;

    @ConfigProperty(name = "app.demo.seed-fixtures", defaultValue = "false")
    boolean seedFixtures;

    @ConfigProperty(name = "app.demo.kickoff-spread-minutes", defaultValue = "60")
    long spreadMinutos;

    public FixtureSeeder(PartidoRepository partidoRepository, PaisRepository paisRepository) {
        this.partidoRepository = partidoRepository;
        this.paisRepository = paisRepository;
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (!seedFixtures) {
            return;
        }
        if (partidoRepository.count() > 0) {
            LOG.info("Ya existen partidos; no se siembra el fixture de demo.");
            return;
        }

        List<Estadio> estadios = Estadio.listAll();
        List<Enfrentamiento> enfrentamientos = new ArrayList<>();
        for (String grupo : GRUPOS) {
            List<Pais> paises = paisRepository.findByGrupo(grupo);
            if (paises.size() < 4) {
                continue;
            }
            for (int k = 0; k < PARES.length; k++) {
                int[] par = PARES[k];
                enfrentamientos.add(new Enfrentamiento(
                        paises.get(par[0]), paises.get(par[1]), grupo, k / 2 + 1));
            }
        }

        // Cada jornada cae en un DIA distinto: J1 hoy (se juega en vivo durante la demo),
        // J2 manana, J3 pasado manana. Asi el filtro por fecha tiene sentido.
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        int porJornada = Math.max(1, enfrentamientos.size() / 3); // ~24
        int[] contadorJornada = new int[4]; // posicion dentro de cada jornada (1..3)

        int idx = 0;
        for (Enfrentamiento e : enfrentamientos) {
            int diaOffset = e.jornada - 1; // 0, 1, 2
            int posicionEnDia = contadorJornada[e.jornada]++;
            OffsetDateTime kickoff;
            if (diaOffset == 0) {
                // Hoy: escalonados desde "ahora" para que se jueguen en vivo, pero sin
                // cruzar medianoche (si no, contarian como del dia siguiente).
                long hastaMedianoche = java.time.Duration.between(
                        ahora, ahora.toLocalDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .getSeconds();
                long ventana = Math.min(spreadMinutos * 60, Math.max(porJornada, hastaMedianoche - 120));
                long intervalo = Math.max(1, ventana / porJornada);
                kickoff = ahora.plusSeconds(posicionEnDia * intervalo);
            } else {
                // Dias futuros: a partir de las 15:00 UTC, escalonados ~12 min (apuestas abiertas).
                kickoff = ahora.plusDays(diaOffset)
                        .withHour(15).withMinute(0).withSecond(0).withNano(0)
                        .plusMinutes(posicionEnDia * 12L);
            }
            Partido p = new Partido();
            p.paisLocal = e.local;
            p.paisVisitante = e.visitante;
            p.grupo = e.grupo;
            p.jornada = e.jornada;
            if (!estadios.isEmpty()) {
                p.estadio = estadios.get(idx % estadios.size());
            }
            p.fechaHora = kickoff;
            p.cierreApuestas = kickoff.minusMinutes(10);
            p.estado = "PROGRAMADO";
            long semilla = Math.abs((e.local.codigoFifa + e.visitante.codigoFifa + e.jornada).hashCode());
            p.idExterno = "demo:" + kickoff.toEpochSecond() + ":" + semilla;
            partidoRepository.persist(p);
            idx++;
        }
        LOG.infof("Fixture de demo sembrado: %d partidos (3 jornadas en 3 dias).", idx);
    }

    private record Enfrentamiento(Pais local, Pais visitante, String grupo, int jornada) {
    }
}
