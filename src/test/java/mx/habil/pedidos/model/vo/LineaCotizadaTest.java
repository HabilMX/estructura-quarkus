package mx.habil.pedidos.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LineaCotizadaTest {

    @Test
    void elImporteEsPrecioPorCantidad() {
        LineaCotizada linea = new LineaCotizada("CAFE-250", 2, 40, new Dinero(18950));

        assertThat(linea.importe()).isEqualTo(new Dinero(37900));
    }

    @Test
    void rechazaUnaLineaSinSku() {
        assertThatThrownBy(() -> new LineaCotizada(" ", 1, 40, new Dinero(100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCantidadCero() {
        assertThatThrownBy(() -> new LineaCotizada("CAFE-250", 0, 40, new Dinero(100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaLineaSinPrecio() {
        assertThatThrownBy(() -> new LineaCotizada("CAFE-250", 1, 40, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
