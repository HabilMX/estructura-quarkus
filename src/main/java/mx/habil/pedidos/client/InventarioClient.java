package mx.habil.pedidos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * El proveedor de afuera: pregunta a otro sistema cuántas piezas hay de un producto y cuánto
 * cuestan.
 *
 * <p>Es el único lugar que sabe que el inventario se consulta por HTTP. La llamada, el tiempo
 * límite y el reintento viven en {@code InventarioApi}; aquí se traducen las fallas de la red a
 * quejas del catálogo: quien lo llama nunca ve una excepción de HTTP.
 */
@ApplicationScoped
public class InventarioClient {

    private final InventarioApi api;

    InventarioClient(@RestClient InventarioApi api) {
        this.api = api;
    }

    /**
     * Consulta un producto.
     *
     * @return el producto, o vacío si el proveedor contesta que no existe
     * @throws PedidosException si el proveedor no contesta a tiempo o no se puede hablar con él
     */
    public Optional<Producto> consultar(String sku) {
        Producto producto;
        try {
            producto = api.producto(sku);
        } catch (WebApplicationException respuesta) {
            if (respuesta.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw new PedidosException(CodigoError.PROVEEDOR_NO_DISPONIBLE,
                    "No se pudo consultar el inventario", respuesta);
        } catch (TimeoutException sinRespuesta) {
            throw new PedidosException(CodigoError.PROVEEDOR_NO_RESPONDE,
                    "El inventario no contestó a tiempo", sinRespuesta);
        } catch (ProcessingException sinConexion) {
            throw new PedidosException(CodigoError.PROVEEDOR_NO_DISPONIBLE,
                    "No se pudo consultar el inventario", sinConexion);
        }
        // Se revisa aquí, ya fuera del reintento: el proveedor sí contestó, y preguntarle otra vez
        // no cambia lo que dijo.
        if (producto != null && !producto.completo()) {
            throw new PedidosException(CodigoError.PROVEEDOR_NO_DISPONIBLE,
                    "El inventario contestó un producto incompleto");
        }
        return Optional.ofNullable(producto);
    }

    /**
     * Lo que contesta el proveedor. Si mañana agrega un campo, se ignora: la lectura no debe
     * romperse por un dato que no usamos. Pero si falta uno que sí usamos, el producto queda
     * incompleto y {@code consultar} lo rechaza: un precio que no llegó no se vuelve cero.
     *
     * <p>Por eso los números son {@code Integer} y {@code Long}, y no {@code int} y {@code long}: a
     * un {@code long} que no llegó, Jackson le pone 0 sin avisar. Un {@code Long} que no llegó es
     * {@code null}, y se nota.
     *
     * <p>El contrato es del proveedor, no nuestro: por eso dice aquí mismo cómo se llama cada campo,
     * con {@code @JsonProperty}, en vez de heredarlo de la configuración de toda la aplicación.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Producto(
            @JsonProperty("sku") String sku,
            @JsonProperty("existencias") Integer existencias,
            @JsonProperty("precio_centavos") Long precioCentavos) {

        /** Si trae los tres datos que se usan. */
        boolean completo() {
            return sku != null && existencias != null && precioCentavos != null;
        }
    }
}
