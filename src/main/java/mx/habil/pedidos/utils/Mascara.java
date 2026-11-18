package mx.habil.pedidos.utils;

/**
 * Tapa datos personales antes de escribirlos en la bitácora.
 *
 * <p>Es una herramienta del cajón: no sabe qué es un pedido y sirve igual en otro proyecto.
 */
public final class Mascara {

    private Mascara() {
    }

    /** {@code ana.lopez@tienda.mx} queda como {@code a***@tienda.mx}. */
    public static String correo(String correo) {
        if (correo == null) {
            return "***";
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
