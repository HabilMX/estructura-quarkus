package mx.habil.pedidos.model.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import mx.habil.pedidos.model.entity.PedidoEntity;

/**
 * La despensa: guarda y trae fichas de pedidos.
 *
 * <p>Es Panache en estilo repositorio: una clase sin código, y Quarkus le pone el cuerpo al
 * construir. De {@code PanacheRepositoryBase} hereda {@code persist}, {@code findByIdOptional} y
 * los demás. Es {@code ...Base} porque el identificador es un {@code UUID}; {@code PanacheRepository}
 * a secas supone un {@code Long}. Aquí no se decide nada: si un pedido se puede guardar o no, lo
 * dice el cocinero ({@code service}).
 */
@ApplicationScoped
public class PedidoRepository implements PanacheRepositoryBase<PedidoEntity, UUID> {
}
