package com.mundial.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cliente HTTP hacia football-data.org. Aisla la dependencia externa y aplica un
 * rate limit propio (8 llamadas/minuto, el plan gratuito permite 10) para no ser
 * bloqueados. Con FOOTBALL_API_KEY="demo" devuelve un marcador simulado para
 * desarrollo sin token.
 */
@ApplicationScoped
public class FootballApiClient {

    private static final Logger LOG = Logger.getLogger(FootballApiClient.class);
    private static final int MAX_LLAMADAS_POR_MINUTO = 8;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    // Ventana de rate limit: contador + marca de inicio (epoch ms).
    private final AtomicInteger llamadasEnVentana = new AtomicInteger(0);
    private final AtomicLong inicioVentana = new AtomicLong(System.currentTimeMillis());

    @ConfigProperty(name = "football.api.base-url", defaultValue = "https://api.football-data.org/v4")
    String baseUrl;

    @ConfigProperty(name = "football.api.key", defaultValue = "demo")
    String apiKey;

    // Cuanto dura (en segundos reales) la simulacion de un partido de 90 min.
    @ConfigProperty(name = "app.demo.match-duration-seconds", defaultValue = "240")
    long demoDuracionSeg;

    /** Marcador externo normalizado al vocabulario interno del sistema. */
    public record MarcadorExterno(Integer golesLocal, Integer golesVisitante, String estado) {
    }

    /**
     * Consulta el marcador de un partido por su id externo.
     * @return vacio si no hay token util, se supero el rate limit, o hubo error de red.
     */
    public Optional<MarcadorExterno> getMarcador(String idExterno) {
        if ("demo".equalsIgnoreCase(apiKey)) {
            return Optional.of(simular(idExterno));
        }
        if (!consumirCupo()) {
            LOG.warnf("Rate limit interno alcanzado (%d/min); se omite GET matches/%s",
                    MAX_LLAMADAS_POR_MINUTO, idExterno);
            return Optional.empty();
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/matches/" + idExterno))
                    .header("X-Auth-Token", apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                LOG.warnf("football-data.org respondio %d para matches/%s", resp.statusCode(), idExterno);
                return Optional.empty();
            }
            return Optional.of(parsear(resp.body()));
        } catch (Exception e) {
            LOG.warnf(e, "Fallo consultando football-data.org matches/%s", idExterno);
            return Optional.empty();
        }
    }

    /**
     * Simulador de demo. El id_externo lleva el kickoff y una semilla:
     * "demo:&lt;kickoffEpochSeconds&gt;:&lt;semilla&gt;". El resultado final es determinista por
     * semilla; los goles aparecen progresivamente segun el tiempo transcurrido (reloj
     * acelerado: 90 min comprimidos en app.demo.match-duration-seconds).
     */
    private MarcadorExterno simular(String idExterno) {
        if (idExterno == null || !idExterno.startsWith("demo:")) {
            return new MarcadorExterno(null, null, "PROGRAMADO");
        }
        String[] partes = idExterno.split(":");
        long kickoff;
        long semilla;
        try {
            kickoff = Long.parseLong(partes[1]);
            semilla = Long.parseLong(partes[2]);
        } catch (RuntimeException e) {
            return new MarcadorExterno(null, null, "PROGRAMADO");
        }

        long transcurrido = Instant.now().getEpochSecond() - kickoff;
        if (transcurrido < 0) {
            return new MarcadorExterno(null, null, "PROGRAMADO"); // aun no empieza
        }

        Random rnd = new Random(semilla);
        int golesLocalFinal = rnd.nextInt(5); // 0-4
        int golesVisitFinal = rnd.nextInt(4); // 0-3
        double[] minutosLocal = fraccionesGoles(rnd, golesLocalFinal);
        double[] minutosVisit = fraccionesGoles(rnd, golesVisitFinal);

        if (transcurrido >= demoDuracionSeg) {
            return new MarcadorExterno(golesLocalFinal, golesVisitFinal, "TERMINADO");
        }
        double progreso = (double) transcurrido / demoDuracionSeg;
        int golesLocal = (int) Arrays.stream(minutosLocal).filter(f -> f <= progreso).count();
        int golesVisit = (int) Arrays.stream(minutosVisit).filter(f -> f <= progreso).count();
        return new MarcadorExterno(golesLocal, golesVisit, "EN_CURSO");
    }

    /** n fracciones ordenadas en (0,1): el momento del partido en que cae cada gol. */
    private double[] fraccionesGoles(Random rnd, int n) {
        double[] f = new double[n];
        for (int i = 0; i < n; i++) {
            f[i] = rnd.nextDouble();
        }
        Arrays.sort(f);
        return f;
    }

    private MarcadorExterno parsear(String body) throws Exception {
        JsonNode root = json.readTree(body);
        JsonNode score = root.path("score").path("fullTime");
        Integer local = score.hasNonNull("home") ? score.get("home").asInt() : null;
        Integer visit = score.hasNonNull("away") ? score.get("away").asInt() : null;
        String estado = mapearEstado(root.path("status").asText(""));
        return new MarcadorExterno(local, visit, estado);
    }

    /** Traduce el status de football-data.org al vocabulario interno. */
    private String mapearEstado(String externo) {
        return switch (externo) {
            case "SCHEDULED", "TIMED" -> "PROGRAMADO";
            case "IN_PLAY", "PAUSED" -> "EN_CURSO";
            case "FINISHED" -> "TERMINADO";
            case "SUSPENDED", "POSTPONED", "CANCELLED" -> "SUSPENDIDO";
            default -> "PROGRAMADO";
        };
    }

    /** Devuelve true si queda cupo en la ventana de 60s; reinicia la ventana al expirar. */
    private synchronized boolean consumirCupo() {
        long ahora = System.currentTimeMillis();
        if (ahora - inicioVentana.get() >= 60_000) {
            inicioVentana.set(ahora);
            llamadasEnVentana.set(0);
        }
        if (llamadasEnVentana.get() >= MAX_LLAMADAS_POR_MINUTO) {
            return false;
        }
        llamadasEnVentana.incrementAndGet();
        return true;
    }
}
