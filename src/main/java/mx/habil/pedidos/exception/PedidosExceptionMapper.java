package mx.habil.pedidos.exception;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Quien atiende las quejas de la cocina: convierte una {@code PedidosException} en Problem Details.
 *
 * <p>Es un {@code ExceptionMapper} de JAX-RS. Hereda de {@code ExceptionMapperBase}, de la
 * extensión de errores, para que su queja pase por el mismo camino que las del marco: le pone la
 * ruta ({@code instance}), el número de la visita ({@code request_id}) y la escribe en la bitácora.
 * Las quejas del marco (validación, JSON mal escrito, ruta que no existe) no pasan por aquí: las
 * atiende la extensión, y {@code CatalogoDeQuejas} les pone el código del catálogo.
 *
 * <p>⚠️ El {@code implements ExceptionMapper<PedidosException>} parece repetido, porque la clase
 * madre ya lo implementa. No sobra: sin él, Quarkus REST no ve que este mapper es para
 * {@code PedidosException} y la queja la atiende el de la extensión para toda {@code Exception},
 * con un 500 (medido: el 404 de un pedido que no existe salía como 500).
 */
@Provider
public class PedidosExceptionMapper extends ExceptionMapperBase<PedidosException>
        implements ExceptionMapper<PedidosException> {

    /** El registro de los pasos por los que pasa toda queja antes de salir; lo entrega CDI. */
    @Inject
    public PedidosExceptionMapper(PostProcessorsRegistry pasos) {
        super(pasos);
    }

    @Override
    protected HttpProblem toProblem(PedidosException queja) {
        return queja.codigo().aProblema(queja.getMessage());
    }
}
