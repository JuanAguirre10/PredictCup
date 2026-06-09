package com.mundial.infrastructure;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Capa de acceso a Redis: cola de apuestas (lista), rankings (sorted set),
 * rate limiting (contadores), locks distribuidos y Pub/Sub.
 *
 * Refina quarkus-redis: lista como cola (cache-list-queue), sorted set para
 * rankings (cache-sorted-set-ranking), lock con SET NX EX (cache-distributed-lock),
 * Pub/Sub para fan-out entre instancias (cache-pubsub-fanout).
 */
@ApplicationScoped
public class RedisService {

    public static final String COLA_APUESTAS = "cola_apuestas";

    private final ListCommands<String, String> lista;
    private final SortedSetCommands<String, String> ranking;
    private final ValueCommands<String, String> valores;
    private final KeyCommands<String> claves;
    private final PubSubCommands<String> pubsub;

    public RedisService(RedisDataSource redis) {
        this.lista = redis.list(String.class);
        this.ranking = redis.sortedSet(String.class);
        this.valores = redis.value(String.class);
        this.claves = redis.key();
        this.pubsub = redis.pubsub(String.class);
    }

    // ---------------------------------------------------------------- Cola

    /** LPUSH cola_apuestas {json}. */
    public void encolarApuesta(String jsonPayload) {
        lista.lpush(COLA_APUESTAS, jsonPayload);
    }

    /** LRANGE cola_apuestas 0 (cantidad-1): inspecciona los siguientes N sin removerlos. */
    public List<String> desencolarApuestas(int cantidad) {
        if (cantidad <= 0) {
            return List.of();
        }
        return lista.lrange(COLA_APUESTAS, 0, cantidad - 1L);
    }

    /** LREM cola_apuestas 1 {json}: remueve la primera coincidencia ya procesada. */
    public void removerApuesta(String jsonPayload) {
        lista.lrem(COLA_APUESTAS, 1, jsonPayload);
    }

    /** LLEN cola_apuestas. */
    public long tamanoColaPendiente() {
        return lista.llen(COLA_APUESTAS);
    }

    // ------------------------------------------------------------- Rankings

    /** ZINCRBY ranking:{scope} puntos {idUsuario}. scope = "global" o "sala:{idSala}". */
    public void actualizarRanking(String scope, UUID idUsuario, int puntos) {
        ranking.zincrby("ranking:" + scope, puntos, idUsuario.toString());
    }

    /** ZREVRANGE ranking:{scope} 0 (topN-1) WITHSCORES. */
    public List<Map<String, Object>> obtenerRanking(String scope, int topN) {
        if (topN <= 0) {
            return List.of();
        }
        List<ScoredValue<String>> filas =
                ranking.zrangeWithScores("ranking:" + scope, 0, topN - 1L, new ZRangeArgs().rev());
        List<Map<String, Object>> resultado = new ArrayList<>(filas.size());
        int posicion = 1;
        for (ScoredValue<String> fila : filas) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("posicion", posicion++);
            entrada.put("idUsuario", fila.value());
            entrada.put("puntos", (long) fila.score());
            resultado.add(entrada);
        }
        return resultado;
    }

    /** ZREVRANK ranking:{scope} {idUsuario} -> posicion 1-based, o null si no esta. */
    public Long posicionEnRanking(String scope, UUID idUsuario) {
        java.util.OptionalLong r = ranking.zrevrank("ranking:" + scope, idUsuario.toString());
        return r.isPresent() ? r.getAsLong() + 1 : null;
    }

    // ----------------------------------------------------------- Rate limit

    /**
     * INCR ratelimit:{idUsuario}; al primer hit fija EXPIRE 60.
     * @return true si el usuario sigue dentro del limite por minuto.
     */
    public boolean verificarRateLimit(UUID idUsuario, int maxPorMinuto) {
        String key = "ratelimit:" + idUsuario;
        long conteo = valores.incr(key);
        if (conteo == 1L) {
            claves.expire(key, 60);
        }
        return conteo <= maxPorMinuto;
    }

    // ----------------------------------------------------------------- Lock

    /** SET lock:{key} 1 NX EX 5. @return true si adquirio el lock. */
    public boolean adquirirLock(String key) {
        return valores.setAndChanged("lock:" + key, "1", new SetArgs().nx().ex(5));
    }

    /** DEL lock:{key}. */
    public void liberarLock(String key) {
        claves.del("lock:" + key);
    }

    // --------------------------------------------------------------- Pub/Sub

    /** PUBLISH canal {mensaje}. */
    public void publicar(String canal, String mensaje) {
        pubsub.publish(canal, mensaje);
    }

    /** SUBSCRIBE canal: invoca handler por cada mensaje recibido (fan-out entre instancias). */
    public void suscribir(String canal, java.util.function.Consumer<String> handler) {
        pubsub.subscribe(canal, handler);
    }
}
