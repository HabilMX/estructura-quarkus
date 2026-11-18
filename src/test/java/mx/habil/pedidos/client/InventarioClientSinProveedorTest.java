package mx.habil.pedidos.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import org.junit.jupiter.api.Test;

/**
 * Cuando en la dirección del proveedor no hay nadie: Quarkus arranca con una dirección donde nadie
 * escucha (un puerto que se pidió libre y se soltó).
 */
@QuarkusTest
@TestProfile(InventarioClientSinProveedorTest.SinNadie.class)
class InventarioClientSinProveedorTest {

    /** La configuración de esta prueba: la del perfil {@code test}, con otra dirección. */
    public static class SinNadie implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return Map.of("quarkus.rest-client.inventario.url", "http://127.0.0.1:" + socket.getLocalPort());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Inject
    InventarioClient cliente;

    @Test
    void siNoHayNadieEnEsaDireccionEsProveedorNoDisponible() {
        assertThatThrownBy(() -> cliente.consultar("CAFE-250"))
                .isInstanceOfSatisfying(PedidosException.class,
                        queja -> assertThat(queja.codigo()).isEqualTo(CodigoError.PROVEEDOR_NO_DISPONIBLE));
    }
}
