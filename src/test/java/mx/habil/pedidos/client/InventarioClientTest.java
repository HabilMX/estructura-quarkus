package mx.habil.pedidos.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import mx.habil.pedidos.client.ProveedorRecurso.ElProveedor;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El proveedor se prueba contra un servidor HTTP de verdad, en esta misma máquina: lo que se
 * quiere ver es qué pasa cuando tarda, cuando no existe y cuando no contesta.
 *
 * <p>Aquí sí se levanta Quarkus: el cliente HTTP, el tiempo límite y el reintento los arma Quarkus
 * a partir de las anotaciones de {@code InventarioApi}, y sin Quarkus no existirían. En las
 * pruebas, el tiempo límite es de 300 ms y la espera entre intentos de 10 ms
 * ({@code %test} en {@code application.yaml}).
 */
@QuarkusTest
@WithTestResource(ProveedorRecurso.class)
class InventarioClientTest {

    private static final String CAFE = """
            {"sku": "CAFE-250", "existencias": 40, "precio_centavos": 18950, "bodega": "norte"}
            """;

    @ElProveedor
    ProveedorDePrueba proveedor;

    @Inject
    InventarioClient cliente;

    @BeforeEach
    void limpiar() {
        proveedor.olvidar();
    }

    @Test
    void leeElProductoEIgnoraLosCamposQueNoConoce() {
        proveedor.contestar(200, CAFE);

        assertThat(cliente.consultar("CAFE-250"))
                .contains(new InventarioClient.Producto("CAFE-250", 40, 18950L));
        assertThat(proveedor.rutas()).containsExactly("/productos/CAFE-250.json");
    }

    @Test
    void unNoExisteDelProveedorEsVacioYNoSeReintenta() {
        proveedor.contestar(404, "");

        assertThat(cliente.consultar("NO-EXISTE")).isEmpty();
        assertThat(proveedor.rutas()).hasSize(1);
    }

    @Test
    void siTardaDeMasSeReintentaYLaSegundaSale() {
        proveedor.tardar(Duration.ofMillis(900), 200, CAFE).contestar(200, CAFE);

        assertThat(cliente.consultar("CAFE-250")).isPresent();
        assertThat(proveedor.rutas()).hasSize(2);
    }

    @Test
    void siNuncaContestaATiempoEsProveedorNoResponde() {
        proveedor.tardar(Duration.ofMillis(900), 200, CAFE)
                .tardar(Duration.ofMillis(900), 200, CAFE)
                .tardar(Duration.ofMillis(900), 200, CAFE);

        assertThatThrownBy(() -> cliente.consultar("CAFE-250"))
                .isInstanceOfSatisfying(PedidosException.class,
                        queja -> assertThat(queja.codigo()).isEqualTo(CodigoError.PROVEEDOR_NO_RESPONDE));
        // Una llamada y dos reintentos: tres en total, ni una más.
        assertThat(proveedor.rutas()).hasSize(3);
    }

    @Test
    void unErrorDelProveedorNoSeReintentaYEsProveedorNoDisponible() {
        proveedor.contestar(500, "");

        assertThatThrownBy(() -> cliente.consultar("CAFE-250"))
                .isInstanceOfSatisfying(PedidosException.class,
                        queja -> assertThat(queja.codigo()).isEqualTo(CodigoError.PROVEEDOR_NO_DISPONIBLE));
        assertThat(proveedor.rutas()).hasSize(1);
    }

    @Test
    void siElProveedorContestaEnCamelCaseLaLecturaFallaYNoSeReintenta() {
        proveedor.contestar(200, """
                {"sku": "CAFE-250", "existencias": 40, "precioCentavos": 18950}
                """);

        // Esta aplicación lee precio_centavos; el proveedor mandó precioCentavos. El precio no
        // llega, y la lectura falla en vez de dejarlo en cero: no se vende nada gratis.
        assertThatThrownBy(() -> cliente.consultar("CAFE-250"))
                .isInstanceOfSatisfying(PedidosException.class,
                        queja -> assertThat(queja.codigo()).isEqualTo(CodigoError.PROVEEDOR_NO_DISPONIBLE));
        assertThat(proveedor.rutas()).hasSize(1);
    }
}
