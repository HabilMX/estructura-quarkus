package mx.habil.pedidos.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import mx.habil.pedidos.facade.PedidoFacade;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.request.CrearPedidoRequest;
import mx.habil.pedidos.model.response.PedidoResponse;
import mx.habil.pedidos.model.vo.EstadoPedido;
import mx.habil.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;

/**
 * Del mesero se prueba lo que es suyo: rutas, códigos, la forma del JSON y que una comanda mal
 * llenada no entre a la cocina. Quarkus se levanta de verdad, con sus filtros y su JSON; la cocina
 * es de mentiras ({@code @InjectMock}).
 */
@QuarkusTest
class PedidoResourceTest {

    private static final UUID ID = UUID.fromString("7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11");
    private static final Instant CREADO = Instant.parse("2026-11-17T18:00:00Z");
    private static final String PROBLEMA = "application/problem+json";

    @InjectMock
    PedidoFacade facade;

    @InjectMock
    PedidoService service;

    @Test
    void crearContesta201ConElTicketEnSnakeCase() {
        when(facade.crear(new CrearPedidoRequest("ana@tienda.mx", List.of(new CrearPedidoRequest.Linea("CAFE-250", 2)))))
                .thenReturn(new PedidoResponse(ID, "RECIBIDO", 37900, CREADO,
                        List.of(new PedidoResponse.Linea("CAFE-250", 2, 18950))));

        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": [{"sku": "CAFE-250", "cantidad": 2}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(201)
                .header("Location", "/v1/pedidos/" + ID)
                .header("X-Request-ID", notNullValue())
                .body("id", equalTo(ID.toString()))
                .body("estado", equalTo("RECIBIDO"))
                .body("total_centavos", equalTo(37900))
                .body("creado_en", equalTo("2026-11-17T18:00:00Z"))
                .body("lineas[0].precio_centavos", equalTo(18950))
                .body("totalCentavos", nullValue());
    }

    @Test
    void unaComandaMalLlenadaNoEntraALaCocina() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "no-es-correo", "lineas": [{"sku": "cafe", "cantidad": 0}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(422)
                .contentType(startsWith(PROBLEMA))
                .body("type", equalTo("https://pedidos.example/errors/solicitud_invalida"))
                .body("title", equalTo("La solicitud no es válida"))
                .body("status", equalTo(422))
                .body("detail", equalTo("La solicitud tiene 3 campo(s) inválido(s)"))
                .body("instance", equalTo("/v1/pedidos"))
                .body("error_code", equalTo("SOLICITUD_INVALIDA"))
                .body("violations", hasSize(3))
                .body("violations.field", containsInAnyOrder(
                        "correo", "lineas[0].sku", "lineas[0].cantidad"));
        verifyNoInteractions(facade, service);
    }

    @Test
    void unaComandaSinLineasTampocoEntra() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": []}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(422)
                .body("violations[0].field", equalTo("lineas"));
        verifyNoInteractions(facade, service);
    }

    @Test
    void unaQuejaDeLaCocinaSaleComoProblemDetails() {
        when(facade.crear(any())).thenThrow(
                new PedidosException(CodigoError.SIN_EXISTENCIAS, "De TAZA-01 se pidieron 4 y hay 3"));

        given().contentType(ContentType.JSON)
                .header("X-Request-ID", "visita-0042")
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": [{"sku": "TAZA-01", "cantidad": 4}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(409)
                .contentType(startsWith(PROBLEMA))
                .header("X-Request-ID", "visita-0042")
                .body("type", equalTo("https://pedidos.example/errors/sin_existencias"))
                .body("detail", equalTo("De TAZA-01 se pidieron 4 y hay 3"))
                .body("error_code", equalTo("SIN_EXISTENCIAS"))
                .body("request_id", equalTo("visita-0042"));
    }

    @Test
    void buscarVaDirectoAlServicioYEntregaElTicketNoLaFicha() {
        when(service.buscar(ID)).thenReturn(new PedidoEntity(ID, "ana@tienda.mx", 4500, EstadoPedido.RECIBIDO,
                CREADO, List.of(new LineaEntity("TAZA-01", 1, 4500))));

        given().when().get("/v1/pedidos/{id}", ID)
                .then().statusCode(200)
                .body("id", equalTo(ID.toString()))
                .body("total_centavos", equalTo(4500))
                .body("correo", nullValue());
        verifyNoInteractions(facade);
    }

    @Test
    void unPedidoQueNoExisteEs404() {
        when(service.buscar(ID)).thenThrow(new PedidosException(CodigoError.PEDIDO_NO_ENCONTRADO,
                "No hay un pedido con el identificador " + ID));

        given().when().get("/v1/pedidos/{id}", ID)
                .then().statusCode(404)
                .body("error_code", equalTo("PEDIDO_NO_ENCONTRADO"))
                .body("instance", equalTo("/v1/pedidos/" + ID));
    }

    @Test
    void unIdentificadorQueNoEsUuidEs404PorqueEsaRutaNoExiste() {
        // Jakarta REST: si un {id} de la ruta no se puede convertir, la ruta no existe (404).
        given().when().get("/v1/pedidos/{id}", "no-es-uuid")
                .then().statusCode(404)
                .contentType(startsWith(PROBLEMA))
                .body("error_code", equalTo("RUTA_NO_ENCONTRADA"));
        verifyNoInteractions(service);
    }

    @Test
    void unJsonMalEscritoEs400SinContarQuePasoPorDentro() {
        given().contentType(ContentType.JSON).body("{ esto no es json")
                .when().post("/v1/pedidos")
                .then().statusCode(400)
                .contentType(startsWith(PROBLEMA))
                .body("error_code", equalTo("SOLICITUD_MAL_FORMADA"))
                .body("title", equalTo("La solicitud no se pudo leer"))
                .body(not(containsString("jackson")));
        verifyNoInteractions(facade);
    }

    @Test
    void unTipoEquivocadoEnUnCampoTampocoCuentaQuePasoPorDentro() {
        // La cantidad debería ser un número. El mensaje de Jackson nombraría la clase de Java.
        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": [{"sku": "CAFE-250", "cantidad": "dos"}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(400)
                .body("error_code", equalTo("SOLICITUD_MAL_FORMADA"))
                .body(not(containsString("java.")))
                .body(not(containsString("mx.habil")));
        verifyNoInteractions(facade);
    }

    @Test
    void loInesperadoEs500SinContarQuePasoPorDentro() {
        when(service.buscar(ID)).thenThrow(new IllegalStateException("la base explotó: jdbc:h2:mem:pedidos"));

        given().when().get("/v1/pedidos/{id}", ID)
                .then().statusCode(500)
                .body("error_code", equalTo("ERROR_INTERNO"))
                .body(not(containsString("jdbc")));
    }

    @Test
    void unaRutaQueNoExisteTambienEsProblemDetails() {
        given().when().get("/v1/no-existe")
                .then().statusCode(404)
                .contentType(startsWith(PROBLEMA))
                .body("error_code", equalTo("RUTA_NO_ENCONTRADA"))
                .body("title", equalTo("La ruta no existe"));
    }
}
