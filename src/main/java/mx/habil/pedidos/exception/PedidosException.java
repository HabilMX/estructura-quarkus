package mx.habil.pedidos.exception;

/**
 * Una queja con su código del catálogo.
 *
 * <p>Es una excepción de Java y nada más: no sabe de HTTP. Quien la lanza solo elige el código y
 * escribe el detalle; {@code PedidosExceptionMapper} la convierte en la respuesta.
 */
public class PedidosException extends RuntimeException {

    private final CodigoError codigo;

    public PedidosException(CodigoError codigo, String detalle) {
        this(codigo, detalle, null);
    }

    public PedidosException(CodigoError codigo, String detalle, Throwable causa) {
        super(detalle, causa);
        this.codigo = codigo;
    }

    public CodigoError codigo() {
        return codigo;
    }
}
