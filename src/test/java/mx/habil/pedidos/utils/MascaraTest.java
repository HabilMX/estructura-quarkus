package mx.habil.pedidos.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MascaraTest {

    @Test
    void dejaLaPrimeraLetraYElDominio() {
        assertThat(Mascara.correo("ana.lopez@tienda.mx")).isEqualTo("a***@tienda.mx");
    }

    @Test
    void loQueNoPareceCorreoSeTapaCompleto() {
        assertThat(Mascara.correo(null)).isEqualTo("***");
        assertThat(Mascara.correo("sin-arroba")).isEqualTo("***");
        assertThat(Mascara.correo("@tienda.mx")).isEqualTo("***");
    }
}
