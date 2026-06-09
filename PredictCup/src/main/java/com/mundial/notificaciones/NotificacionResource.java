package com.mundial.notificaciones;

import com.mundial.notificaciones.dto.NotificacionResponse;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Notificaciones del usuario autenticado. */
@Path("/api/notificaciones")
@Tag(name = "Notificaciones")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    private final NotificacionService service;
    private final JsonWebToken jwt;

    public NotificacionResource(NotificacionService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    private UUID usuarioActual() {
        return UUID.fromString(jwt.getSubject());
    }

    @GET
    @Operation(summary = "Ultimas 20 notificaciones no leidas")
    public List<NotificacionResponse> noLeidas() {
        return service.noLeidas(usuarioActual());
    }

    @PATCH
    @Path("/{id}/leer")
    @Operation(summary = "Marcar una notificacion como leida")
    public Response leer(@PathParam("id") UUID id) {
        service.marcarLeida(usuarioActual(), id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/leer-todas")
    @Operation(summary = "Marcar todas como leidas")
    public Response leerTodas() {
        service.marcarTodasLeidas(usuarioActual());
        return Response.noContent().build();
    }
}
