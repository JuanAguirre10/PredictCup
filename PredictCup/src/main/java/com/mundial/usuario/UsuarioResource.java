package com.mundial.usuario;

import com.mundial.infrastructure.storage.LocalAvatarStorage;
import com.mundial.usuario.dto.ActualizarPerfilRequest;
import com.mundial.usuario.dto.CambiarActivoRequest;
import com.mundial.usuario.dto.CambiarRolRequest;
import com.mundial.usuario.dto.RankingEntryResponse;
import com.mundial.usuario.dto.UsuarioResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Perfil, ranking global y administracion de usuarios. */
@Path("/api/usuarios")
@Tag(name = "Usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    private static final Set<String> TIPOS_IMAGEN = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    private final UsuarioService service;
    private final JsonWebToken jwt;
    private final Instance<LocalAvatarStorage> localStorage;

    public UsuarioResource(UsuarioService service, JsonWebToken jwt,
                           Instance<LocalAvatarStorage> localStorage) {
        this.service = service;
        this.jwt = jwt;
        this.localStorage = localStorage;
    }

    private UUID usuarioActual() {
        return UUID.fromString(jwt.getSubject());
    }

    @GET
    @Path("/yo")
    @Authenticated
    @Operation(summary = "Mi perfil")
    public UsuarioResponse yo() {
        try {
            return service.perfil(usuarioActual());
        } catch (NotFoundException e) {
            // Token válido pero el usuario ya no existe (p. ej. DB reseteada en dev):
            // sesión inválida -> 401 para que el frontend limpie la sesión y vaya al login.
            throw new NotAuthorizedException("Sesión inválida",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    @PATCH
    @Path("/yo")
    @Authenticated
    @Operation(summary = "Editar mi perfil (nombre)")
    public UsuarioResponse editarPerfil(@Valid ActualizarPerfilRequest req) {
        return service.actualizarPerfil(usuarioActual(), req.nombreDisplay());
    }

    @POST
    @Path("/yo/avatar")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir mi foto de perfil")
    public UsuarioResponse subirAvatar(@RestForm("file") FileUpload file) {
        if (file == null) {
            throw new BadRequestException("Falta el archivo (campo 'file')");
        }
        String tipo = file.contentType();
        if (tipo == null || !TIPOS_IMAGEN.contains(tipo)) {
            throw new BadRequestException("Tipo no permitido; usa jpg, png o webp");
        }
        if (file.size() > MAX_BYTES) {
            throw new WebApplicationException("Imagen demasiado grande (max 2 MB)", 413);
        }
        try {
            byte[] datos = Files.readAllBytes(file.uploadedFile());
            String ext = switch (tipo) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
            return service.actualizarAvatar(usuarioActual(), datos, tipo, ext);
        } catch (IOException e) {
            throw new InternalServerErrorException("No se pudo leer la imagen");
        }
    }

    @GET
    @Path("/avatars/{archivo}")
    @PermitAll
    @Produces({"image/png", "image/jpeg", "image/webp"})
    @Operation(summary = "Sirve un avatar guardado localmente")
    public Response avatar(@PathParam("archivo") String archivo) {
        // Allowlist estricto: solo el formato de archivo esperado ({uuid}.{ext}).
        // Bloquea path traversal con / \ : .. y cualquier caracter no esperado.
        if (archivo == null || !archivo.matches("[A-Za-z0-9._-]+")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!localStorage.isResolvable()) {
            return Response.status(Response.Status.NOT_FOUND).build(); // modo R2: lo sirve R2
        }
        return localStorage.get().leer(archivo)
                .map(bytes -> Response.ok(bytes).type(tipoPorExtension(archivo)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    private String tipoPorExtension(String archivo) {
        if (archivo.endsWith(".png")) return "image/png";
        if (archivo.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    @GET
    @Path("/ranking")
    @PermitAll
    @Operation(summary = "Top 50 global enriquecido")
    public List<RankingEntryResponse> ranking() {
        return service.rankingGlobal();
    }

    @GET
    @Path("/ranking/yo")
    @Authenticated
    @Operation(summary = "Mi posición en el ranking global")
    public RankingEntryResponse miRanking() {
        return service.miRanking(usuarioActual());
    }

    // ----------------------------------------------------------- Admin

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "Listar usuarios (admin), con búsqueda opcional")
    public List<UsuarioResponse> listar(@QueryParam("buscar") String buscar) {
        return service.listar(buscar);
    }

    @PATCH
    @Path("/{id}/rol")
    @RolesAllowed("admin")
    @Operation(summary = "Cambiar el rol de un usuario (admin)")
    public UsuarioResponse cambiarRol(@PathParam("id") UUID id, @Valid CambiarRolRequest req) {
        return service.cambiarRol(id, req.rol());
    }

    @PATCH
    @Path("/{id}/activo")
    @RolesAllowed("admin")
    @Operation(summary = "Activar o desactivar un usuario (admin)")
    public UsuarioResponse cambiarActivo(@PathParam("id") UUID id, @Valid CambiarActivoRequest req) {
        return service.cambiarActivo(id, req.activo());
    }
}
