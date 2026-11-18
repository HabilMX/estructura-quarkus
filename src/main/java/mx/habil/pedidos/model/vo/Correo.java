package mx.habil.pedidos.model.vo;

import java.util.Locale;

/**
 * Un correo electrónico ya revisado y en minúsculas.
 *
 * <p>La revisión fina («¿tiene forma de correo?») la hace la comanda, en {@code request}. Aquí
 * solo se garantiza lo que el resto de la cocina da por hecho: que no viene vacío, que trae una
 * arroba y que {@code Ana@Tienda.MX} y {@code ana@tienda.mx} son el mismo cliente.
 */
public record Correo(String valor) {

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("el correo no puede venir vacío");
        }
        valor = valor.strip().toLowerCase(Locale.ROOT);
        int arroba = valor.indexOf('@');
        if (arroba <= 0 || arroba == valor.length() - 1) {
            throw new IllegalArgumentException("el correo necesita algo antes y después de la arroba");
        }
    }
}
