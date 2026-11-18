package mx.habil.pedidos.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Un renglón del pedido tal como se guarda: vive dentro de la ficha del pedido. */
@Embeddable
public record LineaEntity(
        @Column(nullable = false, length = 20) String sku,
        @Column(nullable = false) int cantidad,
        @Column(name = "precio_centavos", nullable = false) long precioCentavos) {
}
