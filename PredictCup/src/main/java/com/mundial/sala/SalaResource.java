package com.mundial.sala;

import com.mundial.sala.dto.CrearSalaRequest;
import com.mundial.sala.dto.SalaResponse;
import com.mundial.sala.dto.SalaResumenResponse;
import com.mundial.sala.dto.UnirseSalaRequest;
import com.mundial.usuario.dto.RankingEntryResponse;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Salas privadas con ranking propio. Requiere usuario autenticado. */
@Path("/api/salas")
@Tag(name = "Salas")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SalaResource {

    private final SalaService service;
    private final JsonWebToken jwt;

    public SalaResource(SalaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    private UUID usuarioActual() {
        return UUID.fromString(jwt.getSubject());
    }

    @POST
    @Operation(summary = "Crear una sala")
    public Response crear(@Valid CrearSalaRequest req) {
        SalaResponse body = service.crear(usuarioActual(), req);
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @POST
    @Path("/unirse")
    @Operation(summary = "Unirse a una sala por codigo")
    public SalaResponse unirse(@Valid UnirseSalaRequest req) {
        return service.unirse(usuarioActual(), req.codigo());
    }

    @GET
    @Path("/mias")
    @Operation(summary = "Mis salas")
    public List<SalaResumenResponse> mias() {
        return service.misSalas(usuarioActual());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Detalle de una sala")
    public SalaResponse detalle(@PathParam("id") UUID id) {
        return service.detalle(id);
    }

    @GET
    @Path("/{id}/ranking")
    @Operation(summary = "Ranking de la sala")
    public List<RankingEntryResponse> ranking(@PathParam("id") UUID id) {
        return service.ranking(id);
    }
}
