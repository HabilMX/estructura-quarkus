package mx.habil.pedidos;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import mx.habil.pedidos.client.ProveedorDePrueba;
import mx.habil.pedidos.client.ProveedorRecurso;
import mx.habil.pedidos.client.ProveedorRecurso.ElProveedor;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La aplicación completa, de punta a punta: Quarkus se levanta en un puerto de pruebas, con su base
 * en memoria, y se le habla por HTTP como le hablaría cualquiera. El único de mentiras es el
 * proveedor de inventario.
 *
 * <p>Es la prueba que ve lo que las demás no: que las piezas encajan y que las llaves de
 * {@code application.yaml} de verdad hacen lo que dicen.
 */
@QuarkusTest
@WithTestResource(ProveedorRecurso.class)
class PedidosDePuntaAPuntaTest {

    private static final String CAFE = """
            {"sku": "CAFE-250", "existencias": 40, "precio_centavos": 18950}
            """;
    private static final String COMANDA = """
            {"correo": "ana@tienda.mx", "lineas": [{"sku": "CAFE-250", "cantidad": 2}]}
            """;

    @ElProveedor
    ProveedorDePrueba proveedor;

    @BeforeEach
    void limpiar() {
        proveedor.olvidar();
    }

    /** El puerto de administración en las pruebas (el 9001, si nadie lo cambió). */
    private static String administracion(String ruta) {
        int puerto = ConfigProvider.getConfig().getValue("quarkus.management.test-port", Integer.class);
        return "http://localhost:" + puerto + ruta;
    }

    @Test
    void unPedidoSeCreaSeGuardaYSePuedeConsultar() {
        proveedor.contestar(200, CAFE);

        Response creado = given().contentType(ContentType.JSON).body(COMANDA).when().post("/v1/pedidos");

        assertThat(creado.statusCode()).isEqualTo(201);
        assertThat(creado.jsonPath().getString("estado")).isEqualTo("RECIBIDO");
        assertThat(creado.jsonPath().getLong("total_centavos")).isEqualTo(37900);
        assertThat(creado.jsonPath().getMap("$")).doesNotContainKey("correo");
        String ubicacion = creado.header("Location");
        assertThat(ubicacion).isEqualTo("/v1/pedidos/" + creado.jsonPath().getString("id"));
        assertThat(creado.header("X-Request-ID")).isNotBlank();

        Response consultado = given().when().get(ubicacion);

        assertThat(consultado.statusCode()).isEqualTo(200);
        assertThat(consultado.jsonPath().getMap("$")).isEqualTo(creado.jsonPath().getMap("$"));
    }

    @Test
    void siElProveedorNoConoceElProductoEs422() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": [{"sku": "NO-EXISTE", "cantidad": 1}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(422)
                .contentType(startsWith("application/problem+json"))
                .body("error_code", equalTo("PRODUCTO_NO_EXISTE"));
    }

    @Test
    void siFaltaUnaPiezaEs409() {
        proveedor.contestar(200, """
                {"sku": "TAZA-01", "existencias": 3, "precio_centavos": 4500}
                """);

        given().contentType(ContentType.JSON)
                .body("""
                        {"correo": "ana@tienda.mx", "lineas": [{"sku": "TAZA-01", "cantidad": 4}]}
                        """)
                .when().post("/v1/pedidos")
                .then().statusCode(409)
                .body("detail", equalTo("De TAZA-01 se pidieron 4 y hay 3"))
                .body("error_code", equalTo("SIN_EXISTENCIAS"));
    }

    @Test
    void siElProveedorTardaMasQueElTiempoLimiteEs504TrasTresIntentos() {
        proveedor.tardar(Duration.ofSeconds(1), 200, CAFE)
                .tardar(Duration.ofSeconds(1), 200, CAFE)
                .tardar(Duration.ofSeconds(1), 200, CAFE);

        given().contentType(ContentType.JSON).body(COMANDA)
                .when().post("/v1/pedidos")
                .then().statusCode(504)
                .body("error_code", equalTo("PROVEEDOR_NO_RESPONDE"))
                .body("type", equalTo("https://pedidos.example/errors/proveedor_no_responde"));
        assertThat(proveedor.rutas()).hasSize(3);
    }

    @Test
    void unPedidoQueNoExisteEs404ConProblemDetails() {
        given().when().get("/v1/pedidos/7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11")
                .then().statusCode(404)
                .contentType(startsWith("application/problem+json"))
                .body("type", equalTo("https://pedidos.example/errors/pedido_no_encontrado"))
                .body("status", equalTo(404))
                .body("instance", equalTo("/v1/pedidos/7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11"));
    }

    @Test
    void laSaludContestaArribaEnElPuertoDeAdministracion() {
        given().when().get(administracion("/q/health/ready"))
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void laSaludNoSePublicaEnElPuertoDeLaApi() {
        given().when().get("/q/health").then().statusCode(404);
        given().when().get("/q/metrics").then().statusCode(404);
    }

    @Test
    void lasMetricasCuentanLosReintentosAlProveedor() {
        proveedor.tardar(Duration.ofSeconds(1), 200, CAFE).contestar(200, CAFE);
        given().contentType(ContentType.JSON).body(COMANDA).when().post("/v1/pedidos").then().statusCode(201);

        given().when().get(administracion("/q/metrics"))
                .then().statusCode(200)
                .body(containsString("ft_retry_retries_total"))
                .body(containsString("http_client_requests_seconds"));
    }
}
