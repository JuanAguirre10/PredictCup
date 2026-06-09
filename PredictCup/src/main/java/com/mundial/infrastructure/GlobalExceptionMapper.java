package com.mundial.infrastructure;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Traduce excepciones de dominio a respuestas HTTP con cuerpo tipo problem-details
 * (quarkus-api api-problem-details). Centraliza el contrato de errores.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception ex) {
        if (ex instanceof ApuestaCerradaException) {
            return problema(400, "Apuestas cerradas", ex.getMessage());
        }
        if (ex instanceof ConflictException) {
            return problema(409, "Conflicto", ex.getMessage());
        }
        if (ex instanceof RateLimitException) {
            return Response.status(429)
                    .header("Retry-After", "60")
                    .type(MediaType.APPLICATION_JSON)
                    .entity(cuerpo(429, "Demasiadas peticiones", ex.getMessage()))
                    .build();
        }
        // Excepciones JAX-RS (NotFound 404, BadRequest 400, 413, etc.): preservar su status.
        if (ex instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            return problema(status, "Error " + status, ex.getMessage());
        }
        // Cualquier otra cosa es un fallo no previsto: 500 y log con stacktrace.
        LOG.error("Error no controlado", ex);
        return problema(500, "Error interno", "Ocurrio un error inesperado");
    }

    private Response problema(int status, String titulo, String detalle) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(cuerpo(status, titulo, detalle))
                .build();
    }

    private Map<String, Object> cuerpo(int status, String titulo, String detalle) {
        return Map.of(
                "status", status,
                "title", titulo,
                "detail", detalle == null ? "" : detalle);
    }
}
