package com.mundial.apuesta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundial.infrastructure.ApuestaCerradaException;
import com.mundial.infrastructure.RedisService;
import com.mundial.partido.Partido;
import com.mundial.partido.PartidoRepository;
import com.mundial.usuario.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Tests unitarios de ApuestaService con Mockito (sin Quarkus ni BD). */
@ExtendWith(MockitoExtension.class)
class ApuestaServiceTest {

    @Mock ApuestaRepository apuestaRepo;
    @Mock PartidoRepository partidoRepo;
    @Mock RedisService redisService;
    @Mock ApuestaMapper mapper;
    @Spy ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @InjectMocks ApuestaService service;

    private final UUID idUsuario = UUID.randomUUID();
    private final UUID idPartido = UUID.randomUUID();

    private Partido partido(boolean abierto, OffsetDateTime fechaHora) {
        Partido p = new Partido();
        p.estado = "PROGRAMADO";
        p.fechaHora = fechaHora;
        p.cierreApuestas = abierto
                ? OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                : OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        return p;
    }

    // -------------------------------------------------------- encolar

    @Test
    void encolarApuesta_ok() {
        when(partidoRepo.findById(idPartido))
                .thenReturn(partido(true, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)));

        service.encolarApuesta(idUsuario, idPartido, 2, 1, "clave-1");

        verify(redisService).encolarApuesta(anyString());
    }

    @Test
    void encolarApuesta_cerrada() {
        when(partidoRepo.findById(idPartido))
                .thenReturn(partido(false, OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)));

        assertThrows(ApuestaCerradaException.class,
                () -> service.encolarApuesta(idUsuario, idPartido, 1, 0, "clave-2"));
        verify(redisService, never()).encolarApuesta(anyString());
    }

    @Test
    void encolarApuesta_noExiste() {
        when(partidoRepo.findById(idPartido)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> service.encolarApuesta(idUsuario, idPartido, 1, 0, "clave-3"));
    }

    // -------------------------------------------------------- persistir

    @Test
    void persistir_idempotente() {
        ApuestaQueueItem item = new ApuestaQueueItem(idUsuario, idPartido, 2, 1, "dup", Instant.now());
        when(apuestaRepo.existsByClaveIdempotencia("dup")).thenReturn(true);

        service.persistirApuesta(item);

        verify(apuestaRepo, never()).persist(any(Apuesta.class));
    }

    @Test
    void persistir_ok() {
        OffsetDateTime fecha = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        ApuestaQueueItem item = new ApuestaQueueItem(idUsuario, idPartido, 2, 1, "ok", Instant.now());
        stubPersistencia(fecha);

        service.persistirApuesta(item);

        verify(apuestaRepo).persist(any(Apuesta.class));
    }

    @Test
    void persistir_anticipada() {
        OffsetDateTime fecha = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        Instant hace25h = fecha.toInstant().minus(25, ChronoUnit.HOURS);
        ApuestaQueueItem item = new ApuestaQueueItem(idUsuario, idPartido, 2, 1, "ant", hace25h);
        stubPersistencia(fecha);

        service.persistirApuesta(item);

        ArgumentCaptor<Apuesta> captor = ArgumentCaptor.forClass(Apuesta.class);
        verify(apuestaRepo).persist(captor.capture());
        assertTrue(captor.getValue().esAnticipada, "25h antes debe ser anticipada");
    }

    @Test
    void persistir_noAnticipada() {
        OffsetDateTime fecha = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        Instant hace5h = fecha.toInstant().minus(5, ChronoUnit.HOURS);
        ApuestaQueueItem item = new ApuestaQueueItem(idUsuario, idPartido, 2, 1, "no-ant", hace5h);
        stubPersistencia(fecha);

        service.persistirApuesta(item);

        ArgumentCaptor<Apuesta> captor = ArgumentCaptor.forClass(Apuesta.class);
        verify(apuestaRepo).persist(captor.capture());
        assertFalse(captor.getValue().esAnticipada, "5h antes NO debe ser anticipada");
    }

    /** Stubs comunes para los tests que sí llegan a persistir. */
    private void stubPersistencia(OffsetDateTime fechaHora) {
        when(apuestaRepo.existsByClaveIdempotencia(anyString())).thenReturn(false);
        when(apuestaRepo.existsByUsuarioAndPartido(idUsuario, idPartido)).thenReturn(false);
        when(partidoRepo.findById(idPartido)).thenReturn(partido(true, fechaHora));
        EntityManager em = mock(EntityManager.class);
        when(apuestaRepo.getEntityManager()).thenReturn(em);
        when(em.getReference(eq(Usuario.class), any())).thenReturn(new Usuario());
    }
}
