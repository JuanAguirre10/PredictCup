package com.mundial.partido;

import com.mundial.apuesta.dto.TopApostadorResponse;
import com.mundial.partido.dto.ActualizarMarcadorRequest;
import com.mundial.partido.dto.AsignarPartidoRequest;
import com.mundial.partido.dto.PartidoResponse;
import com.mundial.partido.dto.PartidoVsResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints de partidos. Lecturas publicas (se enriquecen con la apuesta del
 * usuario si viene autenticado); el PATCH de marcador es solo admin.
 */
@Path("/api/partidos")
@Tag(name = "Partidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartidoResource {

    private final PartidoService service;
    private final JsonWebToken jwt;

    public PartidoResource(PartidoService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /** UUID del usuario si la peticion trae JWT valido; null si es anonima. */
    private UUID usuarioOpcional() {
        try {
            String sub = jwt.getSubject();
            return (sub == null || sub.isBlank()) ? null : UUID.fromString(sub);
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    @PermitAll
    @Operation(summary = "Partidos de una fecha (incluye mi apuesta si autenticado)")
    public List<PartidoResponse> porFecha(@QueryParam("fecha") String fecha) {
        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);
        return service.listarPorFecha(dia, usuarioOpcional());
    }

    @GET
    @Path("/grupo/{grupo}")
    @PermitAll
    @Operation(summary = "Partidos de un grupo con resultados")
    public List<PartidoResponse> porGrupo(@PathParam("grupo") String grupo) {
        return service.listarPorGrupo(grupo, usuarioOpcional());
    }

    @GET
    @Path("/grupo/{grupo}/apostadores")
    @PermitAll
    @Operation(summary = "Top apostadores de un grupo")
    public List<TopApostadorResponse> topApostadores(@PathParam("grupo") String grupo) {
        return service.topApostadoresGrupo(grupo);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Detalle de un partido")
    public PartidoResponse detalle(@PathParam("id") UUID id) {
        return service.detalle(id, usuarioOpcional());
    }

    @GET
    @Path("/{id}/vs")
    @PermitAll
    @Operation(summary = "Vista del enfrentamiento con la tabla del grupo")
    public PartidoVsResponse vs(@PathParam("id") UUID id) {
        return service.vistaEnfrentamiento(id);
    }

    @PATCH
    @Path("/{id}/marcador")
    @RolesAllowed("admin")
    @Operation(summary = "Actualizar marcador y estado (admin)")
    public Response actualizarMarcador(@PathParam("id") UUID id,
                                       @Valid ActualizarMarcadorRequest req) {
        service.actualizarMarcador(id, req.golesLocal(), req.golesVisitante(), req.estado());
        return Response.ok().build();
    }

    // -------- Bracket de eliminatorias --------

    @GET
    @Path("/bracket")
    @PermitAll
    @Operation(summary = "Partidos de eliminatorias (incluye mi apuesta si autenticado)")
    public List<PartidoResponse> bracket() {
        return service.listarBracket(usuarioOpcional());
    }

    @POST
    @Path("/bracket/generar")
    @RolesAllowed("admin")
    @Operation(summary = "Crea los slots vacíos del bracket (admin)")
    public List<PartidoResponse> generarBracket() {
        service.generarBracket();
        return service.listarBracket(null);
    }

    @PATCH
    @Path("/{id}/asignar")
    @RolesAllowed("admin")
    @Operation(summary = "Asigna países/fecha a un slot del bracket (admin)")
    public PartidoResponse asignar(@PathParam("id") UUID id, AsignarPartidoRequest req) {
        return service.asignarPartido(id, req.idPaisLocal(), req.idPaisVisitante(),
                req.idEstadio(), req.fechaHora());
    }
}
