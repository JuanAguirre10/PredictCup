package com.mundial;

import com.mundial.estadio.Estadio;
import com.mundial.pais.Pais;
import com.mundial.partido.Partido;
import com.mundial.partido.TablaPosiciones;
import com.mundial.sala.Sala;
import com.mundial.usuario.Usuario;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Fábricas de entidades para los tests. No persisten: solo construyen objetos
 * coherentes; el test decide cuándo persistir.
 */
public final class TestUtils {

    private TestUtils() {
    }

    public static Usuario makeUsuario(String correo) {
        Usuario u = new Usuario();
        u.googleSub = "sub-" + UUID.randomUUID();
        u.correo = correo;
        u.nombreDisplay = correo.split("@")[0];
        u.rol = Usuario.Rol.USUARIO;
        u.activo = true;
        return u;
    }

    public static Pais makePais(String codigoFifa, String grupo) {
        Pais p = new Pais();
        p.codigoFifa = codigoFifa;
        p.nombre = codigoFifa;
        p.nombreEs = codigoFifa;
        p.banderaEmoji = "🏳"; // bandera generica
        p.confederacion = "FIFA";
        p.grupo = grupo;
        p.eliminado = false;
        return p;
    }

    public static Estadio makeEstadio(String nombre) {
        Estadio e = new Estadio();
        e.nombre = nombre;
        e.ciudad = "Ciudad";
        e.paisSede = "Estados Unidos";
        e.capacidad = 50000;
        return e;
    }

    /**
     * Partido entre dos países. Si {@code abierto}, está PROGRAMADO con cierre en el
     * futuro; si no, ya está cerrado (cierre en el pasado).
     */
    public static Partido makePartido(Pais local, Pais visitante, Estadio estadio, boolean abierto) {
        Partido p = new Partido();
        p.paisLocal = local;
        p.paisVisitante = visitante;
        p.estadio = estadio;
        p.grupo = local.grupo;
        p.estado = "PROGRAMADO";
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        if (abierto) {
            p.fechaHora = ahora.plusDays(2);
            p.cierreApuestas = ahora.plusDays(2);
        } else {
            p.fechaHora = ahora.minusHours(1);
            p.cierreApuestas = ahora.minusHours(2);
        }
        return p;
    }

    public static Sala makeSala(Usuario dueno, String codigo) {
        Sala s = new Sala();
        s.dueno = dueno;
        s.nombre = "Sala de test";
        s.codigo = codigo;
        s.esPublica = false;
        s.maxMiembros = 100;
        return s;
    }

    public static TablaPosiciones makePosicion(Pais pais, String grupo) {
        TablaPosiciones t = new TablaPosiciones();
        t.pais = pais;
        t.grupo = grupo;
        t.posicion = 0;
        return t;
    }
}
