package com.mundial.partido;

import com.mundial.partido.dto.TablaPosicionResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Tabla de posiciones por grupo (publica). */
@Path("/api/posiciones")
@Tag(name = "Posiciones")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
public class TablaPosicionesResource {

    private final PartidoService service;

    public TablaPosicionesResource(PartidoService service) {
        this.service = service;
    }

    @GET
    @Path("/{grupo}")
    @Operation(summary = "Posiciones ordenadas de un grupo")
    public List<TablaPosicionResponse> porGrupo(@PathParam("grupo") String grupo) {
        return service.posicionesPorGrupo(grupo);
    }
}
