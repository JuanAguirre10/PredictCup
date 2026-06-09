package com.mundial.usuario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundial.usuario.dto.UsuarioResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Autenticacion. Implementa el flujo OAuth de Google MANUALMENTE (sin usar quarkus-oidc
 * como mecanismo de auth) para no chocar con el JWT propio que valida mp-jwt en /api:
 *   1) GET /auth/callback  -> redirige a la pantalla de Google (con state en cookie).
 *   2) GET /auth/oidc?code  -> intercambia el code por tokens, lee el id_token (email/sub/
 *      nombre/foto), hace upsert del usuario, emite NUESTRO JWT y redirige al frontend.
 * Tambien expone /auth/dev (login local sin Google).
 */
@Path("/auth")
@Tag(name = "Auth")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);
    private static final String GOOGLE_AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN = "https://oauth2.googleapis.com/token";
    private static final String STATE_COOKIE = "oauth_state";

    private final UsuarioService usuarioService;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "http://localhost:8080")
    String issuer;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:3000/login/callback")
    String frontendUrl;

    @ConfigProperty(name = "app.jwt.duracion-horas", defaultValue = "12")
    long duracionHoras;

    @ConfigProperty(name = "app.auth.dev-login.enabled", defaultValue = "false")
    boolean devLoginEnabled;

    @ConfigProperty(name = "app.google.client-id", defaultValue = "")
    String googleClientId;

    @ConfigProperty(name = "app.google.client-secret", defaultValue = "")
    String googleClientSecret;

    @ConfigProperty(name = "app.google.redirect-uri", defaultValue = "http://localhost:8080/auth/oidc")
    String googleRedirectUri;

    public AuthResource(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1) Inicio del login con Google: redirige a la pantalla de consentimiento.
    @GET
    @Path("/callback")
    @PermitAll
    @Operation(summary = "Inicia el login con Google")
    public Response callback() {
        if (googleClientId.isBlank()) {
            throw new BadRequestException("Google no está configurado (app.google.client-id)");
        }
        String state = UUID.randomUUID().toString();
        String url = GOOGLE_AUTH
                + "?response_type=code"
                + "&client_id=" + enc(googleClientId)
                + "&redirect_uri=" + enc(googleRedirectUri)
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state)
                + "&prompt=select_account";
        NewCookie cookie = new NewCookie.Builder(STATE_COOKIE).value(state)
                .path("/").httpOnly(true).maxAge(600).build();
        return Response.seeOther(URI.create(url)).cookie(cookie).build();
    }

    // 2) Google vuelve aquí con el code. Intercambiamos, leemos el id_token y emitimos JWT.
    @GET
    @Path("/oidc")
    @PermitAll
    @Operation(summary = "Callback de Google: emite el JWT propio y redirige al frontend")
    public Response googleCallback(@QueryParam("code") String code,
                                   @QueryParam("state") String state,
                                   @CookieParam(STATE_COOKIE) String stateCookie) {
        if (code == null || code.isBlank()) {
            return redirigirError("sin_code");
        }
        if (state == null || !state.equals(stateCookie)) {
            return redirigirError("state_invalido");
        }
        try {
            JsonNode idToken = intercambiarCode(code);
            String sub = idToken.path("sub").asText(null);
            String correo = idToken.path("email").asText(null);
            String nombre = idToken.path("name").asText(null);
            String avatar = idToken.path("picture").asText(null);

            UsuarioResponse usuario = usuarioService.upsertDesdeOAuth(sub, correo, nombre, avatar);
            LOG.infof("Login Google OK usuario=%s correo=%s", usuario.id(), correo);
            return Response.seeOther(URI.create(frontendUrl + "?token=" + emitirToken(usuario)))
                    .cookie(borrarStateCookie())
                    .build();
        } catch (Exception e) {
            LOG.error("Fallo en el intercambio OAuth con Google", e);
            return redirigirError("intercambio_fallido");
        }
    }

    /** POST al token endpoint de Google y devuelve el id_token decodificado (claims). */
    private JsonNode intercambiarCode(String code) throws Exception {
        String body = "code=" + enc(code)
                + "&client_id=" + enc(googleClientId)
                + "&client_secret=" + enc(googleClientSecret)
                + "&redirect_uri=" + enc(googleRedirectUri)
                + "&grant_type=authorization_code";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Google token endpoint respondió " + resp.statusCode() + ": " + resp.body());
        }
        String idToken = json.readTree(resp.body()).path("id_token").asText(null);
        if (idToken == null) {
            throw new IllegalStateException("Respuesta de Google sin id_token");
        }
        // El id_token es un JWT: decodificamos el payload (viene por TLS server-to-server,
        // confiable; en producción real conviene además verificar la firma con el JWKS).
        String[] partes = idToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        return json.readTree(payload);
    }

    // --- Dev-login (solo local, sin Google) ---
    @GET
    @Path("/dev")
    @PermitAll
    @Operation(summary = "Login de desarrollo sin Google (solo si app.auth.dev-login.enabled=true)")
    public Response devLogin(@QueryParam("email") String email, @QueryParam("nombre") String nombre) {
        if (!devLoginEnabled) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("status", 403, "title", "Dev-login deshabilitado",
                            "detail", "Activa app.auth.dev-login.enabled solo en local"))
                    .build();
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Falta el parametro ?email=");
        }
        String correo = email.trim();
        String display = (nombre == null || nombre.isBlank()) ? correo.split("@")[0] : nombre;
        UsuarioResponse usuario = usuarioService.upsertDesdeOAuth("dev-" + correo, correo, display, null);
        LOG.infof("Dev-login usuario=%s correo=%s rol=%s", usuario.id(), correo, usuario.rol());
        return Response.seeOther(URI.create(frontendUrl + "?token=" + emitirToken(usuario))).build();
    }

    /** Firma un JWT propio para el usuario (sub = id interno, groups = rol). */
    private String emitirToken(UsuarioResponse usuario) {
        return Jwt.issuer(issuer)
                .subject(usuario.id().toString())
                .upn(usuario.correo())
                .groups(Set.of(usuario.rol()))
                .claim("nombre", usuario.nombreDisplay())
                .expiresIn(Duration.ofHours(duracionHoras))
                .sign();
    }

    private Response redirigirError(String motivo) {
        // Redirige al login del frontend con el motivo (el frontend puede mostrarlo).
        String base = frontendUrl.replace("/login/callback", "/login");
        return Response.seeOther(URI.create(base + "?error=" + motivo)).cookie(borrarStateCookie()).build();
    }

    private NewCookie borrarStateCookie() {
        return new NewCookie.Builder(STATE_COOKIE).value("").path("/").maxAge(0).build();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
