package com.mundial.sala;

import com.mundial.usuario.Usuario;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SalaResourceTest {

    static final String USER = "11111111-1111-1111-1111-111111111111";
    static final UUID USER_ID = UUID.fromString(USER);

    @BeforeEach
    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            MiembroSala.deleteAll();
            Sala.deleteAll();
            Usuario.deleteAll();
            insertarUsuario(USER_ID, "auth@test.local");
        });
    }

    /** Inserta un usuario con id forzado (native query: evita que @UuidGenerator lo sobrescriba). */
    private void insertarUsuario(UUID id, String correo) {
        Panache.getEntityManager().createNativeQuery(
                        "insert into usuarios (id, google_sub, correo, nombre_display, rol, "
                                + "puntos_totales, racha_actual, activo, creado_en, actualizado_en) "
                                + "values (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10)")
                .setParameter(1, id)
                .setParameter(2, "sub-" + id)
                .setParameter(3, correo)
                .setParameter(4, "Test")
                .setParameter(5, "USUARIO")
                .setParameter(6, 0)
                .setParameter(7, 0)
                .setParameter(8, Boolean.TRUE)
                .setParameter(9, OffsetDateTime.now(ZoneOffset.UTC))
                .setParameter(10, OffsetDateTime.now(ZoneOffset.UTC))
                .executeUpdate();
    }

    /** Crea una sala (con dueño nuevo distinto al usuario autenticado) y devuelve su código. */
    private String crearSalaDeOtro(String codigo, boolean autenticadoEsMiembro) {
        QuarkusTransaction.requiringNew().run(() -> {
            UUID dueno = UUID.randomUUID();
            insertarUsuario(dueno, "dueno-" + codigo + "@test.local");
            Sala s = new Sala();
            s.dueno = Panache.getEntityManager().getReference(Usuario.class, dueno);
            s.nombre = "Sala " + codigo;
            s.codigo = codigo;
            s.esPublica = false;
            s.maxMiembros = 100;
            s.persist();
            new MiembroSala(new MiembroSalaId(s.id, dueno)).persist();
            if (autenticadoEsMiembro) {
                new MiembroSala(new MiembroSalaId(s.id, USER_ID)).persist();
            }
        });
        return codigo;
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void crearSala_201_conCodigo6() {
        given().contentType("application/json")
                .body("{\"nombre\":\"Mi Sala\",\"descripcion\":\"x\",\"esPublica\":false,\"maxMiembros\":100}")
                .when().post("/api/salas")
                .then().statusCode(201)
                .body("codigo.length()", is(6));
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void unirse_codigoValido_200() {
        String codigo = crearSalaDeOtro("JOIN01", false);
        given().contentType("application/json").body("{\"codigo\":\"" + codigo + "\"}")
                .when().post("/api/salas/unirse")
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void unirse_codigoInvalido_404() {
        given().contentType("application/json").body("{\"codigo\":\"ZZZZZZ\"}")
                .when().post("/api/salas/unirse")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void unirse_yaMiembro_409() {
        String codigo = crearSalaDeOtro("MEMB01", true);
        given().contentType("application/json").body("{\"codigo\":\"" + codigo + "\"}")
                .when().post("/api/salas/unirse")
                .then().statusCode(409);
    }
}
