package mx.habil.pedidos.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DineroTest {

    @Test
    void sumaYMultiplicaSinPerderCentavos() {
        Dinero cafe = new Dinero(18950);

        assertThat(cafe.por(2).mas(new Dinero(4500))).isEqualTo(new Dinero(42400));
    }

    @Test
    void seEscribeEnPesosConDosDecimales() {
        assertThat(new Dinero(18950).enPesos()).isEqualTo("$189.50");
        assertThat(new Dinero(5).enPesos()).isEqualTo("$0.05");
        assertThat(Dinero.CERO.enPesos()).isEqualTo("$0.00");
    }

    @Test
    void noAceptaDineroNegativo() {
        assertThatThrownBy(() -> new Dinero(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("el dinero no puede ser negativo");
    }

    @Test
    void noAceptaCantidadNegativa() {
        Dinero cafe = new Dinero(18950);

        assertThatThrownBy(() -> cafe.por(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fallaEnVezDeDesbordarse() {
        Dinero enorme = new Dinero(Long.MAX_VALUE);

        assertThatThrownBy(() -> enorme.mas(new Dinero(1))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> enorme.por(2)).isInstanceOf(ArithmeticException.class);
    }
}
