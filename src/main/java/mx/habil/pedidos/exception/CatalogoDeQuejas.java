package mx.habil.pedidos.exception;

import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.ProblemContext;
import io.quarkiverse.httpproblem.postprocessing.ProblemPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;

/**
 * Le pone el código del catálogo a toda queja que levanta el marco.
 *
 * <p>La extensión de errores ya contesta con Problem Details las quejas del marco: una ruta que no
 * existe, un JSON mal escrito, una comanda mal llenada. Pero las contesta con su título en inglés,
 * sin {@code error_code} y, a veces, con un {@code detail} que cuenta qué pasó por dentro (el nombre
 * de una clase de Java, por ejemplo). Este paso corre antes de que la queja salga y SIEMPRE cambia
 * el tipo, el título y el detalle por los del catálogo. Siempre, no «cuando parezca peligroso»: una
 * regla que decide caso por caso deja pasar el caso que nadie pensó.
 *
 * <p>Las quejas de la cocina ya traen su {@code error_code}: a ésas no las toca.
 */
@ApplicationScoped
public class CatalogoDeQuejas implements ProblemPostProcessor {

    static final String VIOLACIONES = "violations";

    @Override
    public HttpProblem apply(HttpProblem problema, ProblemContext contexto) {
        if (problema.getParameters().containsKey(CodigoError.CAMPO)) {
            return problema;
        }
        Object violaciones = problema.getParameters().get(VIOLACIONES);
        CodigoError codigo = violaciones instanceof Collection<?>
                ? CodigoError.SOLICITUD_INVALIDA
                : CodigoError.delMarco(problema.getStatusCode());
        return codigo.completar(HttpProblem.builder(problema), detalle(codigo, violaciones))
                // El estado se conserva: si el catálogo no tiene uno igual, no se inventa otro.
                .withStatus(codigo == CodigoError.SOLICITUD_INVALIDA ? codigo.estado() : problema.getStatusCode())
                .build();
    }

    /** El detalle, escrito aquí y nunca copiado de la excepción. */
    private static String detalle(CodigoError codigo, Object violaciones) {
        return switch (codigo) {
            case SOLICITUD_INVALIDA -> "La solicitud tiene %d campo(s) inválido(s)"
                    .formatted(violaciones instanceof Collection<?> lista ? lista.size() : 0);
            case RUTA_NO_ENCONTRADA -> "No hay nada en esta ruta";
            case METODO_NO_PERMITIDO -> "Esta ruta no atiende ese método";
            case TIPO_NO_ADMITIDO -> "Manda el cuerpo como application/json";
            case ERROR_INTERNO -> "Inténtalo de nuevo; si se repite, repórtalo con el X-Request-ID de esta respuesta";
            default -> "Revisa que el cuerpo sea JSON válido y que cada valor tenga el tipo que se espera";
        };
    }

    /** Antes que la bitácora de la extensión (prioridad 0): así se registra ya con el catálogo. */
    @Override
    public int priority() {
        return 50;
    }
}
