package mx.habil.pedidos.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkiverse.httpproblem.HttpProblem;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CodigoErrorTest {

    @Test
    void armaElCuerpoConLaFormaDeProblemDetails() {
        HttpProblem problema = CodigoError.PEDIDO_NO_ENCONTRADO.aProblema("No hay un pedido con ese identificador");

        assertThat(problema.getType()).isEqualTo(URI.create("https://pedidos.example/errors/pedido_no_encontrado"));
        assertThat(problema.getTitle()).isEqualTo("El pedido no existe");
        assertThat(problema.getStatusCode()).isEqualTo(404);
        assertThat(problema.getDetail()).isEqualTo("No hay un pedido con ese identificador");
        assertThat(problema.getParameters()).containsEntry("error_code", "PEDIDO_NO_ENCONTRADO");
    }

    @ParameterizedTest
    @EnumSource(CodigoError.class)
    void cadaCodigoTieneSuTipoYSuCodigoPlano(CodigoError codigo) {
        HttpProblem problema = codigo.aProblema("detalle");

        assertThat(problema.getType().toString()).startsWith(CodigoError.BASE).isLowerCase();
        assertThat(problema.getParameters()).containsEntry("error_code", codigo.name());
        assertThat(problema.getStatusCode()).isEqualTo(codigo.estado());
    }

    @Test
    void unaQuejaDelMarcoSeBuscaPorSuEstado() {
        assertThat(CodigoError.delMarco(400)).isEqualTo(CodigoError.SOLICITUD_MAL_FORMADA);
        assertThat(CodigoError.delMarco(404)).isEqualTo(CodigoError.RUTA_NO_ENCONTRADA);
        assertThat(CodigoError.delMarco(405)).isEqualTo(CodigoError.METODO_NO_PERMITIDO);
        // Sin un código para ese estado: un 5xx es error interno; cualquier otro, mal formada.
        assertThat(CodigoError.delMarco(502)).isEqualTo(CodigoError.ERROR_INTERNO);
        assertThat(CodigoError.delMarco(406)).isEqualTo(CodigoError.SOLICITUD_MAL_FORMADA);
    }

    @Test
    void unaQuejaDeLaCocinaNoSeConfundeConUnaDelMarco() {
        // PEDIDO_NO_ENCONTRADO también es 404, pero es de la cocina: el marco no la usa.
        assertThat(CodigoError.delMarco(404)).isNotEqualTo(CodigoError.PEDIDO_NO_ENCONTRADO);
        assertThat(CodigoError.delMarco(503)).isEqualTo(CodigoError.ERROR_INTERNO);
    }

    @Test
    void laExcepcionLlevaSuCodigoYSuDetalle() {
        PedidosException queja = new PedidosException(CodigoError.SIN_EXISTENCIAS, "De CAFE-250 se pidieron 5 y hay 2");

        assertThat(queja.codigo()).isEqualTo(CodigoError.SIN_EXISTENCIAS);
        assertThat(queja.getMessage()).isEqualTo("De CAFE-250 se pidieron 5 y hay 2");
    }
}
