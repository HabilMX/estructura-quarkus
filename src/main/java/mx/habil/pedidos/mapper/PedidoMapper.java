package mx.habil.pedidos.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.response.PedidoResponse;

/**
 * El que pasa en limpio: copia de la ficha del archivo al ticket del cliente.
 *
 * <p>Solo copia campos. Si un día aquí aparece un {@code if} que decide algo del negocio, ese
 * {@code if} es del cocinero ({@code service}).
 */
@ApplicationScoped
public class PedidoMapper {

    public PedidoResponse aResponse(PedidoEntity pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getEstado().name(),
                pedido.getTotalCentavos(),
                pedido.getCreadoEn(),
                pedido.getLineas().stream()
                        .map(l -> new PedidoResponse.Linea(l.sku(), l.cantidad(), l.precioCentavos()))
                        .toList());
    }
}
