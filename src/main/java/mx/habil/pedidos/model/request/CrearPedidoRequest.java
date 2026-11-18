package mx.habil.pedidos.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La comanda: lo que el cliente pide, tal como llega en el JSON.
 *
 * <p>Las anotaciones dicen qué forma debe tener. Quien las revisa es el mesero ({@code api}),
 * antes de que la comanda entre a la cocina. La comanda no trae precios: el precio lo pone el
 * proveedor, nunca el cliente.
 */
public record CrearPedidoRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotEmpty @Size(max = 20) List<@Valid @NotNull Linea> lineas) {

    /** Un renglón de la comanda: qué producto y cuántas piezas. */
    public record Linea(
            @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,20}") String sku,
            @Positive @Max(99) int cantidad) {
    }
}
