package mx.habil.pedidos.model.vo;

/**
 * Una cantidad de dinero, en centavos.
 *
 * <p>Es una medida exacta: con {@code long} no hay centavos que se pierdan, como pasa con
 * {@code double}. Y nunca es negativa: un {@code Dinero} que existe ya es válido, y quien lo
 * recibe no tiene que volver a revisarlo.
 */
public record Dinero(long centavos) {

    public static final Dinero CERO = new Dinero(0);

    public Dinero {
        if (centavos < 0) {
            throw new IllegalArgumentException("el dinero no puede ser negativo");
        }
    }

    /** Suma dos cantidades. Si el resultado no cabe, falla en vez de dar un número equivocado. */
    public Dinero mas(Dinero otro) {
        return new Dinero(Math.addExact(centavos, otro.centavos));
    }

    /** El precio de varias piezas iguales. */
    public Dinero por(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("la cantidad no puede ser negativa");
        }
        return new Dinero(Math.multiplyExact(centavos, (long) cantidad));
    }

    /** Para leerlo: 18950 centavos son {@code $189.50}. */
    public String enPesos() {
        return "$%d.%02d".formatted(centavos / 100, centavos % 100);
    }
}
