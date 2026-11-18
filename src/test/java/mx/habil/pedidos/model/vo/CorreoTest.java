package mx.habil.pedidos.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CorreoTest {

    @Test
    void quitaEspaciosYPasaAMinusculas() {
        assertThat(new Correo("  Ana@Tienda.MX ").valor()).isEqualTo("ana@tienda.mx");
    }

    @Test
    void dosFormasDeEscribirloSonElMismoCliente() {
        assertThat(new Correo("Ana@Tienda.MX")).isEqualTo(new Correo("ana@tienda.mx"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "sin-arroba", "@tienda.mx", "ana@"})
    void rechazaLoQueNoEsUnCorreo(String valor) {
        assertThatThrownBy(() -> new Correo(valor)).isInstanceOf(IllegalArgumentException.class);
    }
}
