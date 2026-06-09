package com.mundial.apuesta;

import com.mundial.TestUtils;
import com.mundial.estadio.Estadio;
import com.mundial.pais.Pais;
import com.mundial.partido.Partido;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class ApuestaResourceTest {

    static final String USER = "11111111-1111-1111-1111-111111111111";

    UUID partidoAbierto;
    UUID partidoCerrado;

    @BeforeEach
    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            Partido.deleteAll();
            Pais.deleteAll();
            Estadio.deleteAll();

            Pais a = TestUtils.makePais("AAA", "C");
            a.persist();
            Pais b = TestUtils.makePais("BBB", "C");
            b.persist();
            Estadio e = TestUtils.makeEstadio("Estadio Test");
            e.persist();

            Partido abierto = TestUtils.makePartido(a, b, e, true);
            abierto.persist();
            partidoAbierto = abierto.id;

            Partido cerrado = TestUtils.makePartido(a, b, e, false);
            cerrado.persist();
            partidoCerrado = cerrado.id;
        });
    }

    private String body(UUID idPartido, String clave) {
        return "{\"idPartido\":\"" + idPartido + "\",\"golesLocal\":2,\"golesVisitante\":1,"
                + "\"claveIdempotencia\":\"" + clave + "\"}";
    }

    @Test
    void post_sinAuth_401() {
        given().contentType("application/json").body(body(partidoAbierto, "c1"))
                .when().post("/api/apuestas")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void post_partidoNoExiste_404() {
        given().contentType("application/json").body(body(UUID.randomUUID(), "c2"))
                .when().post("/api/apuestas")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void post_partidoCerrado_400() {
        given().contentType("application/json").body(body(partidoCerrado, "c3"))
                .when().post("/api/apuestas")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void post_valida_202() {
        given().contentType("application/json").body(body(partidoAbierto, "c4"))
                .when().post("/api/apuestas")
                .then().statusCode(202);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void post_duplicado_202_idempotente() {
        String b = body(partidoAbierto, "c5-dup");
        given().contentType("application/json").body(b)
                .when().post("/api/apuestas").then().statusCode(202);
        // Misma clave: el encolado sigue devolviendo 202 (la idempotencia se aplica al persistir).
        given().contentType("application/json").body(b)
                .when().post("/api/apuestas").then().statusCode(202);
    }

    @Test
    void getMias_sinAuth_401() {
        given().when().get("/api/apuestas/mias")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = USER, roles = {"user"})
    @JwtSecurity(claims = @Claim(key = "sub", value = USER))
    void getMias_conAuth_200() {
        given().when().get("/api/apuestas/mias")
                .then().statusCode(200);
    }
}
