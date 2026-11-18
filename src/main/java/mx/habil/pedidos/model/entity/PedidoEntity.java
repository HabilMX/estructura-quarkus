package mx.habil.pedidos.model.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.habil.pedidos.model.vo.EstadoPedido;

/**
 * La ficha del archivo: el pedido tal como se guarda en la base de datos.
 *
 * <p>Es una clase y no un {@code record} porque JPA necesita un constructor sin argumentos y
 * poder llenar los campos después. Tiene la forma de la tabla, no la del JSON: por eso nunca
 * sale por la puerta. Lo que sale es el ticket, {@code PedidoResponse}.
 *
 * <p>No hereda de {@code PanacheEntity}. Con esa herencia, la ficha sabría guardarse sola
 * ({@code pedido.persist()}) y cualquier capa que la tocara podría escribir en la base. Aquí la
 * ficha es solo datos; quien guarda y trae es la despensa, {@code PedidoRepository}.
 */
@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(name = "total_centavos", nullable = false)
    private long totalCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    // Las líneas se traen junto con el pedido: son pocas y siempre se usan.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_lineas", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicion")
    private List<LineaEntity> lineas = new ArrayList<>();

    /** Para JPA. El código de la aplicación usa el otro constructor. */
    protected PedidoEntity() {
    }

    public PedidoEntity(UUID id, String correo, long totalCentavos, EstadoPedido estado,
            Instant creadoEn, List<LineaEntity> lineas) {
        this.id = id;
        this.correo = correo;
        this.totalCentavos = totalCentavos;
        this.estado = estado;
        this.creadoEn = creadoEn;
        this.lineas = new ArrayList<>(lineas);
    }

    public UUID getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public long getTotalCentavos() {
        return totalCentavos;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public List<LineaEntity> getLineas() {
        return List.copyOf(lineas);
    }
}
