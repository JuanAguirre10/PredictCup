package com.mundial.admin;

import com.mundial.admin.dto.ColaStatsResponse;
import com.mundial.apuesta.ColaApuestasLog;
import com.mundial.infrastructure.RedisService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Endpoints de administración (solo rol admin). */
@Path("/api/admin")
@Tag(name = "Admin")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final RedisService redisService;

    public AdminResource(RedisService redisService) {
        this.redisService = redisService;
    }

    @GET
    @Path("/cola/stats")
    @Operation(summary = "Métricas de la cola de apuestas (pendientes, procesados hoy, fallidos)")
    public ColaStatsResponse colaStats() {
        long pendientes = redisService.tamanoColaPendiente();
        OffsetDateTime inicioHoy = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        long procesadosHoy = ColaApuestasLog.count(
                "estado = ?1 and procesadoEn >= ?2", "PROCESADO", inicioHoy);
        long fallidos = ColaApuestasLog.count("estado", "FALLIDO");
        return new ColaStatsResponse(pendientes, procesadosHoy, fallidos);
    }
}
