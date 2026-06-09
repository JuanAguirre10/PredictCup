package com.mundial.infrastructure;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Rate limit a nivel HTTP solo para POST /api/apuestas: cuenta por usuario
 * autenticado en Redis (30/min). Si excede, corta con 429 + Retry-After: 60.
 *
 * Es la barrera de entrada; el servicio confia en este filtro y no vuelve a
 * incrementar el contador (evita doble conteo). Refina quarkus-api api-rate-limit.
 */
@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int MAX_POR_MINUTO = 30;

    private final RedisService redisService;

    public RateLimitFilter(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!aplica(ctx)) {
            return;
        }
        SecurityContext sec = ctx.getSecurityContext();
        Principal principal = sec == null ? null : sec.getUserPrincipal();
        if (principal == null) {
            // Sin identidad no se puede limitar por usuario; deja que la capa de
            // seguridad responda 401 mas adelante.
            return;
        }
        UUID idUsuario;
        try {
            idUsuario = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!redisService.verificarRateLimit(idUsuario, MAX_POR_MINUTO)) {
            ctx.abortWith(Response.status(429)
                    .header("Retry-After", "60")
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of(
                            "status", 429,
                            "title", "Demasiadas peticiones",
                            "detail", "Maximo " + MAX_POR_MINUTO + " apuestas por minuto"))
                    .build());
        }
    }

    private boolean aplica(ContainerRequestContext ctx) {
        if (!"POST".equalsIgnoreCase(ctx.getMethod())) {
            return false;
        }
        String path = ctx.getUriInfo().getPath();
        // Normaliza para aceptar "api/apuestas" y "/api/apuestas".
        return path != null && path.replaceFirst("^/", "").equals("api/apuestas");
    }
}
