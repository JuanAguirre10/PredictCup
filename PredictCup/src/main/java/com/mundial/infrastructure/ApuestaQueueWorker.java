package com.mundial.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundial.apuesta.ApuestaQueueItem;
import com.mundial.apuesta.ApuestaService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Worker que drena la cola Redis y persiste las apuestas en lotes. El tamano del
 * lote (20) es igual al pool Agroal (datasource.jdbc.max-size): nunca pide mas
 * conexiones de las que hay, asi se mete la presion en la cola y no en la BD.
 *
 * Refina quarkus-event-driven (evt-queue-worker-drain): cada item se procesa de
 * forma aislada; un fallo se loguea y no detiene al resto del lote.
 */
@ApplicationScoped
public class ApuestaQueueWorker {

    private static final Logger LOG = Logger.getLogger(ApuestaQueueWorker.class);
    private static final int LOTE = 20; // == pool Agroal

    private final RedisService redisService;
    private final ApuestaService apuestaService;
    private final ObjectMapper json;

    public ApuestaQueueWorker(RedisService redisService,
                              ApuestaService apuestaService,
                              ObjectMapper json) {
        this.redisService = redisService;
        this.apuestaService = apuestaService;
        this.json = json;
    }

    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void procesarCola() {
        List<String> items = redisService.desencolarApuestas(LOTE);
        if (items.isEmpty()) {
            return;
        }
        int ok = 0, fallidos = 0;
        for (String raw : items) {
            long inicio = System.currentTimeMillis();
            ApuestaQueueItem item;
            try {
                item = json.readValue(raw, ApuestaQueueItem.class);
            } catch (Exception e) {
                // JSON corrupto: lo sacamos de la cola para no reprocesarlo en bucle.
                LOG.errorf(e, "Item de cola ilegible, descartado: %s", raw);
                redisService.removerApuesta(raw);
                fallidos++;
                continue;
            }
            try {
                apuestaService.persistirApuesta(item);
                redisService.removerApuesta(raw);
                apuestaService.registrarProcesoCola(item, "PROCESADO",
                        System.currentTimeMillis() - inicio, null);
                ok++;
            } catch (Exception e) {
                // Fallo persistiendo: se registra y se deja en la cola para reintento.
                LOG.warnf(e, "Fallo persistiendo apuesta clave=%s; se reintentara",
                        item.claveIdempotencia());
                apuestaService.registrarProcesoCola(item, "FALLIDO",
                        System.currentTimeMillis() - inicio, e.getMessage());
                fallidos++;
            }
        }
        if (ok > 0 || fallidos > 0) {
            LOG.debugf("Lote procesado: %d ok, %d fallidos, %d pendientes",
                    ok, fallidos, redisService.tamanoColaPendiente());
        }
    }
}
