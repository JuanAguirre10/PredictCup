package com.mundial.partido;

import com.mundial.TestUtils;
import com.mundial.estadio.Estadio;
import com.mundial.pais.Pais;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class PartidoResourceTest {

    UUID partidoId;
    String fechaPartido;

    @BeforeEach
    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            com.mundial.partido.Partido.deleteAll();
            TablaPosiciones.deleteAll();
            Pais.deleteAll();
            Estadio.deleteAll();

            Pais a = TestUtils.makePais("AAA", "C");
            a.persist();
            Pais b = TestUtils.makePais("BBB", "C");
            b.persist();
            Estadio e = TestUtils.makeEstadio("Estadio Test");
            e.persist();

            TablaPosiciones t1 = TestUtils.makePosicion(a, "C");
            t1.puntos = 6;
            t1.diferenciaGoles = 3;
            t1.persist();
            TablaPosiciones t2 = TestUtils.makePosicion(b, "C");
            t2.puntos = 3;
            t2.diferenciaGoles = 1;
            t2.persist();

            Partido p = TestUtils.makePartido(a, b, e, false);
            p.persist();
            partidoId = p.id;
            fechaPartido = p.fechaHora.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString();
        });
    }

    @Test
    void getPartidos_200() {
        given().when().get("/api/partidos?fecha=" + LocalDate.now())
                .then().statusCode(200);
    }

    @Test
    void getPosicionesGrupo_200_ordenado() {
        given().when().get("/api/posiciones/C")
                .then().statusCode(200)
                .body("size()", is(2))
                .body("[0].puntos", is(6))   // ordenado por puntos desc
                .body("[1].puntos", is(3));
    }

    @Test
    void getVs_200_conDatosCompletos() {
        given().when().get("/api/partidos/" + partidoId + "/vs")
                .then().statusCode(200)
                .body("paisLocal.codigoFifa", is("AAA"))
                .body("paisVisitante.codigoFifa", is("BBB"))
                .body("estadio", notNullValue())
                .body("tablaPosicionesGrupo.size()", is(2));
    }

    @Test
    @TestSecurity(user = "noadmin", roles = {"user"})
    void patchMarcador_sinAdmin_403() {
        given().contentType("application/json")
                .body("{\"golesLocal\":1,\"golesVisitante\":0,\"estado\":\"EN_CURSO\"}")
                .when().patch("/api/partidos/" + partidoId + "/marcador")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void patchMarcador_conAdmin_200() {
        given().contentType("application/json")
                .body("{\"golesLocal\":2,\"golesVisitante\":1,\"estado\":\"TERMINADO\"}")
                .when().patch("/api/partidos/" + partidoId + "/marcador")
                .then().statusCode(200);
    }
}
