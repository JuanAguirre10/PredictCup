package com.mundial.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket de rankings en vivo. Un cliente se conecta a un "scope":
 *   /ws/ranking/global        -> ranking global
 *   /ws/ranking/sala:{idSala} -> ranking de una sala
 *
 * El fan-out es cross-instancia (cache-pubsub-fanout): la puntuacion publica en
 * Redis "ranking:actualizado"; cada instancia recibe el evento y reenvia a SUS
 * sesiones. Las sesiones son estaticas (compartidas entre instancias del endpoint).
 *
 * Nota de arquitectura: los @ServerEndpoint los instancia el contenedor WebSocket,
 * por eso aqui se usa @Inject de campo (no constructor) como excepcion documentada.
 */
@ApplicationScoped
@ServerEndpoint("/ws/ranking/{scope}")
public class RankingWebSocket {

    private static final Logger LOG = Logger.getLogger(RankingWebSocket.class);

    // scope -> sesiones conectadas a ese scope.
    private static final Map<String, CopyOnWriteArraySet<Session>> SESIONES = new ConcurrentHashMap<>();
    private static final AtomicBoolean SUSCRITO = new AtomicBoolean(false);

    @Inject
    RedisService redisService;

    @Inject
    ObjectMapper json;

    @PostConstruct
    void suscribirUnaVez() {
        // Solo la primera instancia del endpoint registra la suscripcion Pub/Sub.
        if (SUSCRITO.compareAndSet(false, true)) {
            redisService.suscribir("ranking:actualizado", this::onRankingActualizado);
            LOG.info("RankingWebSocket suscrito a ranking:actualizado");
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("scope") String scope) {
        SESIONES.computeIfAbsent(scope, k -> new CopyOnWriteArraySet<>()).add(session);
        LOG.debugf("WS abierto scope=%s sesiones=%d", scope, SESIONES.get(scope).size());
    }

    @OnClose
    public void onClose(Session session, @PathParam("scope") String scope) {
        remover(scope, session);
    }

    @OnError
    public void onError(Session session, @PathParam("scope") String scope, Throwable error) {
        LOG.warnf(error, "Error en WS scope=%s", scope);
        remover(scope, session);
    }

    private void remover(String scope, Session session) {
        CopyOnWriteArraySet<Session> set = SESIONES.get(scope);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                SESIONES.remove(scope);
            }
        }
    }

    /** Llega un idPartido por Pub/Sub: refresca el ranking global y el de cada sala conectada. */
    private void onRankingActualizado(String idPartido) {
        try {
            broadcast("global", rankingJson("global"));
            for (String scope : SESIONES.keySet()) {
                if (scope.startsWith("sala:")) {
                    broadcast(scope, rankingJson(scope));
                }
            }
        } catch (Exception e) {
            LOG.warnf(e, "No se pudo difundir ranking para partido=%s", idPartido);
        }
    }

    private String rankingJson(String scope) throws Exception {
        return json.writeValueAsString(redisService.obtenerRanking(scope, 50));
    }

    void broadcast(String scope, String payload) {
        CopyOnWriteArraySet<Session> set = SESIONES.get(scope);
        if (set == null) {
            return;
        }
        for (Session s : set) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(payload);
            }
        }
    }
}
