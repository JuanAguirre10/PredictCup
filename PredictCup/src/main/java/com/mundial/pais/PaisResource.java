package com.mundial.pais;

import com.mundial.pais.dto.PaisResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Catalogo publico de paises del mundial. */
@Path("/api/paises")
@Tag(name = "Paises")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
public class PaisResource {

    private final PaisService service;

    public PaisResource(PaisService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Todos los paises")
    public List<PaisResponse> todos() {
        return service.listarTodos();
    }

    @GET
    @Path("/grupo/{grupo}")
    @Operation(summary = "Paises de un grupo")
    public List<PaisResponse> porGrupo(@PathParam("grupo") String grupo) {
        return service.porGrupo(grupo);
    }
}
