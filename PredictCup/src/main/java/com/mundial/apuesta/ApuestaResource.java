package com.mundial.apuesta;

import com.mundial.apuesta.dto.ApuestaAceptadaResponse;
import com.mundial.apuesta.dto.ApuestaResponse;
import com.mundial.apuesta.dto.ConteoApuestasResponse;
import com.mundial.apuesta.dto.CrearApuestaRequest;
import com.mundial.apuesta.dto.EstadisticasApuestasResponse;
import com.mundial.apuesta.dto.HistorialApuestaResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de apuestas. Resource fino: extrae identidad, delega en el servicio,
 * devuelve DTOs. Toda la regla de negocio vive en ApuestaService.
 */
@Path("/api/apuestas")
@Tag(name = "Apuestas")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApuestaResource {

    private final ApuestaService service;
    private final JsonWebToken jwt;

    public ApuestaResource(ApuestaService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    private UUID usuarioActual() {
        return UUID.fromString(jwt.getSubject());
    }

    @POST
    @Operation(summary = "Encolar una apuesta (devuelve 202; se persiste async)")
    public Response crear(@Valid CrearApuestaRequest req) {
        service.encolarApuesta(usuarioActual(), req.idPartido(),
                req.golesLocal(), req.golesVisitante(), req.claveIdempotencia());
        return Response.status(Response.Status.ACCEPTED)
                .entity(ApuestaAceptadaResponse.aceptada(req.claveIdempotencia()))
                .build();
    }

    @GET
    @Path("/mias")
    @Operation(summary = "Mis apuestas")
    public List<ApuestaResponse> mias() {
        return service.misApuestas(usuarioActual());
    }

    @GET
    @Path("/partido/{idPartido}")
    @Operation(summary = "Mi apuesta para un partido")
    public ApuestaResponse enPartido(@PathParam("idPartido") UUID idPartido) {
        return service.miApuestaEnPartido(usuarioActual(), idPartido);
    }

    @GET
    @Path("/estadisticas")
    @Operation(summary = "Estadísticas de mis apuestas")
    public EstadisticasApuestasResponse estadisticas() {
        return service.estadisticasUsuario(usuarioActual());
    }

    @GET
    @Path("/historial")
    @Operation(summary = "Mi historial de predicciones (limite configurable, máx 200)")
    public List<HistorialApuestaResponse> historial(
            @QueryParam("limite") @DefaultValue("20") int limite) {
        return service.historialUsuario(usuarioActual(), Math.min(Math.max(limite, 1), 200));
    }

    @GET
    @Path("/partido/{idPartido}/conteo")
    @RolesAllowed("admin")
    @Operation(summary = "Cuántas apuestas hay para un partido (admin)")
    public ConteoApuestasResponse conteo(@PathParam("idPartido") UUID idPartido) {
        return new ConteoApuestasResponse(service.contarPorPartido(idPartido));
    }
}
