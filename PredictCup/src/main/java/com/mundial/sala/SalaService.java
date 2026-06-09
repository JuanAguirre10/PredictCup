package com.mundial.sala;

import com.mundial.infrastructure.ConflictException;
import com.mundial.sala.dto.CrearSalaRequest;
import com.mundial.sala.dto.SalaResponse;
import com.mundial.sala.dto.SalaResumenResponse;
import com.mundial.usuario.Usuario;
import com.mundial.usuario.UsuarioService;
import com.mundial.usuario.dto.RankingEntryResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/** Reglas de salas privadas: crear (con codigo de invitacion), unirse, ranking. */
@ApplicationScoped
public class SalaService {

    private static final Logger LOG = Logger.getLogger(SalaService.class);
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RANKING_TOP = 50;

    private final SalaRepository repository;
    private final SalaMapper mapper;
    private final UsuarioService usuarioService;

    public SalaService(SalaRepository repository, SalaMapper mapper, UsuarioService usuarioService) {
        this.repository = repository;
        this.mapper = mapper;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public SalaResponse crear(UUID idDueno, CrearSalaRequest req) {
        Sala sala = new Sala();
        sala.dueno = repository.getEntityManager().getReference(Usuario.class, idDueno);
        sala.nombre = req.nombre();
        sala.descripcion = req.descripcion();
        sala.esPublica = req.esPublica();
        sala.maxMiembros = req.maxMiembros() == null ? 100 : req.maxMiembros();
        sala.codigo = generarCodigoUnico();
        repository.persist(sala);
        sala.idDueno = idDueno; // espejo read-only: lo fijamos en memoria para la respuesta

        // El dueno es miembro automaticamente.
        new MiembroSala(new MiembroSalaId(sala.id, idDueno)).persist();

        LOG.infof("Sala creada %s codigo=%s dueno=%s", sala.id, sala.codigo, idDueno);
        return mapper.toResponse(sala);
    }

    @Transactional
    public SalaResponse unirse(UUID idUsuario, String codigo) {
        Sala sala = repository.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundException("No existe una sala con ese codigo"));
        if (repository.esMiembro(sala.id, idUsuario)) {
            throw new ConflictException("Ya eres miembro de esta sala");
        }
        if (repository.contarMiembros(sala.id) >= sala.maxMiembros) {
            throw new ConflictException("La sala alcanzo su maximo de miembros");
        }
        new MiembroSala(new MiembroSalaId(sala.id, idUsuario)).persist();
        LOG.infof("Usuario %s se unio a sala %s", idUsuario, sala.id);
        return mapper.toResponse(sala);
    }

    /** Salas del usuario con su conteo de miembros. */
    @Transactional
    public List<SalaResumenResponse> misSalas(UUID idUsuario) {
        return repository.salasDeUsuario(idUsuario).stream()
                .map(s -> new SalaResumenResponse(
                        s.id, s.nombre, s.codigo, s.descripcion, s.esPublica,
                        repository.contarMiembros(s.id),
                        idUsuario.equals(s.idDueno)))
                .toList();
    }

    /** Detalle de una sala (nombre, código, etc.). */
    public SalaResponse detalle(UUID idSala) {
        Sala sala = repository.findById(idSala);
        if (sala == null) {
            throw new NotFoundException("Sala no encontrada: " + idSala);
        }
        return mapper.toResponse(sala);
    }

    /** Ranking de la sala (Redis) enriquecido con datos de usuario. */
    public List<RankingEntryResponse> ranking(UUID idSala) {
        if (repository.findById(idSala) == null) {
            throw new NotFoundException("Sala no encontrada: " + idSala);
        }
        return usuarioService.rankingEnriquecido("sala:" + idSala, RANKING_TOP);
    }

    private String generarCodigoUnico() {
        for (int intento = 0; intento < 20; intento++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            }
            String codigo = sb.toString();
            if (repository.findByCodigo(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new ConflictException("No se pudo generar un codigo de sala unico");
    }
}
