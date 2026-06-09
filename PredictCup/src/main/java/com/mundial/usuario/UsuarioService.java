package com.mundial.usuario;

import com.mundial.apuesta.ApuestaService;
import com.mundial.infrastructure.RedisService;
import com.mundial.infrastructure.storage.AvatarStorage;
import com.mundial.usuario.dto.RankingEntryResponse;
import com.mundial.usuario.dto.UsuarioResponse;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestion de usuarios: upsert OAuth, perfil, ranking enriquecido y administracion. */
@ApplicationScoped
public class UsuarioService {

    private static final Logger LOG = Logger.getLogger(UsuarioService.class);
    private static final int RANKING_TOP = 50;

    private final UsuarioRepository repository;
    private final UsuarioMapper mapper;
    private final RedisService redisService;
    private final ApuestaService apuestaService;
    private final AvatarStorage avatarStorage;
    private final List<String> adminEmails;

    public UsuarioService(UsuarioRepository repository, UsuarioMapper mapper,
                          RedisService redisService, ApuestaService apuestaService,
                          Instance<AvatarStorage> avatarStorage,
                          @ConfigProperty(name = "app.admin.emails", defaultValue = "") List<String> adminEmails) {
        this.repository = repository;
        this.mapper = mapper;
        this.redisService = redisService;
        this.apuestaService = apuestaService;
        // @LookupIfProperty selecciona en runtime la implementacion activa (local | r2).
        this.avatarStorage = avatarStorage.get();
        this.adminEmails = adminEmails;
    }

    /** Edita los datos editables del perfil (por ahora, el nombre para mostrar). */
    @Transactional
    public UsuarioResponse actualizarPerfil(UUID idUsuario, String nombreDisplay) {
        Usuario u = buscar(idUsuario);
        if (nombreDisplay != null && !nombreDisplay.isBlank()) {
            u.nombreDisplay = nombreDisplay.trim();
        }
        return mapper.toResponse(u);
    }

    /** Sube la foto al storage y guarda su URL en el usuario. */
    @Transactional
    public UsuarioResponse actualizarAvatar(UUID idUsuario, byte[] datos, String contentType, String extension) {
        Usuario u = buscar(idUsuario);
        String url = avatarStorage.subir(idUsuario + "." + extension, datos, contentType);
        // Cache-buster: el archivo se sobrescribe con el mismo nombre, así que sin esto el
        // navegador seguiría mostrando la foto anterior (misma URL).
        u.urlAvatar = url + (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
        LOG.infof("Avatar actualizado usuario=%s -> %s", idUsuario, u.urlAvatar);
        return mapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse upsertDesdeOAuth(String googleSub, String correo, String nombre, String avatar) {
        // Busca por el sub de OAuth y, si no, por correo (identidad estable): así el mismo
        // correo logueado por distintos métodos (dev-login / Google) no crea duplicados.
        Usuario u = repository.findByGoogleSub(googleSub)
                .or(() -> (correo != null && !correo.isBlank())
                        ? repository.findByCorreo(correo)
                        : java.util.Optional.empty())
                .orElseGet(Usuario::new);
        boolean nuevo = u.id == null;
        if (nuevo) {
            u.correo = correo;
            // Solo al crear tomamos nombre/foto de OAuth; luego se respetan las ediciones del usuario.
            u.nombreDisplay = nombre != null ? nombre : (correo != null ? correo : "Usuario");
            u.urlAvatar = avatar;
        }
        u.googleSub = googleSub; // vincula/actualiza el identificador OAuth actual
        // El rol se decide en CADA login por la allowlist (autoritativo).
        u.rol = esAdmin(correo) ? Usuario.Rol.ADMIN : Usuario.Rol.USUARIO;
        repository.persist(u);
        LOG.infof("Usuario %s (%s) %s rol=%s", u.id, correo, nuevo ? "creado" : "actualizado", u.rol);
        return mapper.toResponse(u);
    }

    private boolean esAdmin(String correo) {
        return correo != null && adminEmails.stream().anyMatch(e -> e.equalsIgnoreCase(correo.trim()));
    }

    public UsuarioResponse perfil(UUID idUsuario) {
        return mapper.toResponse(buscar(idUsuario));
    }

    /** Top 50 global (Redis) enriquecido con nombre, avatar, racha y nº de apuestas. */
    public List<RankingEntryResponse> rankingGlobal() {
        return rankingEnriquecido("global", RANKING_TOP);
    }

    /** Ranking de cualquier scope (global o sala:{id}) enriquecido con datos de usuario. */
    @Transactional
    public List<RankingEntryResponse> rankingEnriquecido(String scope, int top) {
        List<Map<String, Object>> base = redisService.obtenerRanking(scope, top);
        if (base.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = base.stream()
                .map(m -> UUID.fromString((String) m.get("idUsuario")))
                .toList();
        Map<UUID, Usuario> usuarios = repository.list("id in ?1", ids).stream()
                .collect(Collectors.toMap(u -> u.id, Function.identity()));
        Map<UUID, Long> conteos = apuestaService.contarPorUsuarios(ids);

        List<RankingEntryResponse> filas = new ArrayList<>(base.size());
        for (Map<String, Object> m : base) {
            UUID id = UUID.fromString((String) m.get("idUsuario"));
            Usuario u = usuarios.get(id);
            int posicion = ((Number) m.get("posicion")).intValue();
            int puntos = ((Number) m.get("puntos")).intValue();
            filas.add(new RankingEntryResponse(
                    posicion, id,
                    u != null ? u.nombreDisplay : "—",
                    u != null ? u.urlAvatar : null,
                    puntos,
                    u != null ? u.rachaActual : 0,
                    conteos.getOrDefault(id, 0L)));
        }
        return filas;
    }

    /** Mi fila de ranking aunque no esté en el top 50 (posicion via ZREVRANK). */
    public RankingEntryResponse miRanking(UUID idUsuario) {
        Usuario u = buscar(idUsuario);
        Long pos = redisService.posicionEnRanking("global", idUsuario);
        long apuestas = apuestaService.contarPorUsuarios(List.of(idUsuario)).getOrDefault(idUsuario, 0L);
        return new RankingEntryResponse(
                pos == null ? 0 : pos.intValue(),
                u.id, u.nombreDisplay, u.urlAvatar, u.puntosTotales, u.rachaActual, apuestas);
    }

    // -------------------------------------------------------- Administracion

    public List<UsuarioResponse> listar(String busqueda) {
        List<Usuario> us;
        if (busqueda == null || busqueda.isBlank()) {
            us = repository.listAll(Sort.by("puntosTotales").descending());
        } else {
            String patron = "%" + busqueda.toLowerCase() + "%";
            us = repository.list("lower(nombreDisplay) like ?1 or lower(correo) like ?1", patron);
        }
        return us.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarRol(UUID idUsuario, String rol) {
        Usuario u = buscar(idUsuario);
        u.rol = "admin".equalsIgnoreCase(rol) ? Usuario.Rol.ADMIN : Usuario.Rol.USUARIO;
        LOG.infof("Rol de %s -> %s", idUsuario, u.rol);
        return mapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse cambiarActivo(UUID idUsuario, boolean activo) {
        Usuario u = buscar(idUsuario);
        u.activo = activo;
        LOG.infof("Usuario %s activo=%s", idUsuario, activo);
        return mapper.toResponse(u);
    }

    private Usuario buscar(UUID idUsuario) {
        Usuario u = repository.findById(idUsuario);
        if (u == null) {
            throw new NotFoundException("Usuario no encontrado: " + idUsuario);
        }
        return u;
    }
}
