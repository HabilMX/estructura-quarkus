package mx.habil.pedidos.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El ticket: lo que el cliente recibe.
 *
 * <p>Solo lleva lo que el cliente necesita ver. El correo se queda en la ficha del archivo: un
 * dato personal no sale si nadie lo pidió. En el JSON, {@code totalCentavos} sale como
 * {@code total_centavos}: lo decide una sola línea de {@code application.yaml}.
 */
public record PedidoResponse(
        UUID id,
        String estado,
        long totalCentavos,
        Instant creadoEn,
        List<Linea> lineas) {

    /** Un renglón del ticket, ya con su precio. */
    public record Linea(String sku, int cantidad, long precioCentavos) {
    }
}
