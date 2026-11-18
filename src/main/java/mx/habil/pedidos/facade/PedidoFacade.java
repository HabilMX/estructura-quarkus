package mx.habil.pedidos.facade;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import mx.habil.pedidos.client.InventarioClient;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import mx.habil.pedidos.mapper.PedidoMapper;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.request.CrearPedidoRequest;
import mx.habil.pedidos.model.response.PedidoResponse;
import mx.habil.pedidos.model.vo.Correo;
import mx.habil.pedidos.model.vo.Dinero;
import mx.habil.pedidos.model.vo.LineaCotizada;
import mx.habil.pedidos.service.PedidoService;

/**
 * El jefe de cocina: coordina a los que intervienen para recibir un pedido.
 *
 * <p>Existe porque aquí intervienen dos: el proveedor de inventario ({@code client}) y el
 * cocinero ({@code service}). Dice en qué orden trabaja cada uno, y nada más: no calcula, no
 * decide y no guarda. Consultar un pedido no pasa por aquí: lo atiende un solo servicio, y el
 * mesero lo llama directo.
 *
 * <p>La llamada al proveedor va fuera de la transacción: la base no se queda esperando mientras
 * el proveedor contesta. La transacción empieza y termina dentro del cocinero.
 */
@ApplicationScoped
public class PedidoFacade {

    private final InventarioClient inventario;
    private final PedidoService pedidos;
    private final PedidoMapper mapper;

    public PedidoFacade(InventarioClient inventario, PedidoService pedidos, PedidoMapper mapper) {
        this.inventario = inventario;
        this.pedidos = pedidos;
        this.mapper = mapper;
    }

    public PedidoResponse crear(CrearPedidoRequest comanda) {
        // 1. Al proveedor: ¿cuántas piezas hay de cada producto y a qué precio?
        List<LineaCotizada> cotizadas = new ArrayList<>();
        for (CrearPedidoRequest.Linea linea : comanda.lineas()) {
            InventarioClient.Producto producto = inventario.consultar(linea.sku())
                    .orElseThrow(() -> new PedidosException(CodigoError.PRODUCTO_NO_EXISTE,
                            "No hay un producto con el sku " + linea.sku()));
            cotizadas.add(new LineaCotizada(linea.sku(), linea.cantidad(), producto.existencias(),
                    new Dinero(producto.precioCentavos())));
        }
        // 2. Al cocinero: aplica las reglas y guarda el pedido.
        PedidoEntity pedido = pedidos.crear(new Correo(comanda.correo()), cotizadas);
        // 3. Se pasa en limpio: de la ficha del archivo al ticket.
        return mapper.aResponse(pedido);
    }
}
