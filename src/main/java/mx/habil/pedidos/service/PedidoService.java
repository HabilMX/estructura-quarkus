package mx.habil.pedidos.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.repository.PedidoRepository;
import mx.habil.pedidos.model.vo.Correo;
import mx.habil.pedidos.model.vo.Dinero;
import mx.habil.pedidos.model.vo.EstadoPedido;
import mx.habil.pedidos.model.vo.LineaCotizada;
import mx.habil.pedidos.utils.Mascara;
import org.jboss.logging.Logger;

/**
 * El cocinero: aquí viven las reglas de un pedido.
 *
 * <p>Las reglas son dos: no se acepta un pedido si falta alguna pieza, y el total es la suma de
 * precio por cantidad. No sabe de HTTP ni de JSON: recibe medidas exactas ({@code vo}) y entrega
 * la ficha del archivo ({@code entity}). Por eso se puede probar sin levantar Quarkus.
 */
@ApplicationScoped
public class PedidoService {

    private static final Logger log = Logger.getLogger(PedidoService.class);

    private final PedidoRepository pedidos;
    private final Clock reloj;

    public PedidoService(PedidoRepository pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    /** Recibe un pedido: revisa las existencias, calcula el total y lo guarda. */
    @Transactional
    public PedidoEntity crear(Correo cliente, List<LineaCotizada> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("un pedido necesita al menos una línea");
        }
        Dinero total = Dinero.CERO;
        for (LineaCotizada linea : lineas) {
            if (linea.cantidad() > linea.existencias()) {
                throw new PedidosException(CodigoError.SIN_EXISTENCIAS,
                        "De %s se pidieron %d y hay %d".formatted(linea.sku(), linea.cantidad(), linea.existencias()));
            }
            total = total.mas(linea.importe());
        }
        PedidoEntity pedido = new PedidoEntity(
                UUID.randomUUID(),
                cliente.valor(),
                total.centavos(),
                EstadoPedido.RECIBIDO,
                // La hora se corta al milisegundo. El reloj de Linux da nanosegundos y la base
                // guarda menos: sin el corte, el ticket de hoy y el de mañana dirían horas distintas.
                reloj.instant().truncatedTo(ChronoUnit.MILLIS),
                lineas.stream()
                        .map(l -> new LineaEntity(l.sku(), l.cantidad(), l.precioUnitario().centavos()))
                        .toList());
        pedidos.persist(pedido);
        // El correo es un dato personal: a la bitácora llega tapado.
        log.infof("pedido %s recibido de %s por %s", pedido.getId(), Mascara.correo(cliente.valor()),
                total.enPesos());
        return pedido;
    }

    /** Trae un pedido por su identificador. Si no existe, es una queja del catálogo, no un null. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public PedidoEntity buscar(UUID id) {
        return pedidos.findByIdOptional(id)
                .orElseThrow(() -> new PedidosException(CodigoError.PEDIDO_NO_ENCONTRADO,
                        "No hay un pedido con el identificador " + id));
    }
}
