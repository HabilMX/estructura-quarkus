package mx.habil.pedidos.model.vo;

/**
 * Un renglón del pedido con lo que contestó el proveedor: cuántas piezas hay y a qué precio.
 *
 * <p>Es lo que el jefe de cocina le pasa al cocinero. No es la comanda (no trae el precio: el
 * precio nunca lo dice el cliente) ni la ficha del archivo.
 */
public record LineaCotizada(String sku, int cantidad, int existencias, Dinero precioUnitario) {

    public LineaCotizada {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("la línea necesita un sku");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("la cantidad debe ser mayor que cero");
        }
        if (precioUnitario == null) {
            throw new IllegalArgumentException("la línea necesita un precio");
        }
    }

    /** Lo que cuesta este renglón: precio por cantidad. */
    public Dinero importe() {
        return precioUnitario.por(cantidad);
    }
}
