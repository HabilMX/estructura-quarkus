package mx.habil.pedidos.exception;

import io.quarkiverse.httpproblem.HttpProblem;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

/**
 * El libro de quejas: el catálogo de todo lo que esta API puede contestar cuando algo sale mal.
 *
 * <p>Cada queja tiene un código fijo y su estado HTTP. Y aquí está el único lugar donde se arma
 * el cuerpo del error, con la forma de Problem Details (RFC 9457). Los primeros cinco son quejas
 * que levanta el marco (una ruta que no existe, un JSON mal escrito); los demás, la cocina.
 */
public enum CodigoError {

    SOLICITUD_MAL_FORMADA(400, "La solicitud no se pudo leer", true),
    RUTA_NO_ENCONTRADA(404, "La ruta no existe", true),
    METODO_NO_PERMITIDO(405, "El método no está permitido en esta ruta", true),
    TIPO_NO_ADMITIDO(415, "El tipo de contenido no se admite", true),
    SOLICITUD_INVALIDA(422, "La solicitud no es válida", true),
    PEDIDO_NO_ENCONTRADO(404, "El pedido no existe", false),
    PRODUCTO_NO_EXISTE(422, "El producto no existe", false),
    SIN_EXISTENCIAS(409, "No hay existencias suficientes", false),
    PROVEEDOR_NO_RESPONDE(504, "El proveedor no contestó a tiempo", false),
    PROVEEDOR_NO_DISPONIBLE(503, "No se pudo consultar al proveedor", false),
    ERROR_INTERNO(500, "Ocurrió un error inesperado", true);

    /** La dirección base de los tipos de error. Cámbiala por el dominio de tu producto. */
    static final String BASE = "https://pedidos.example/errors/";

    /** El campo propio que va en cada error, además de los del RFC. */
    public static final String CAMPO = "error_code";

    private final int estado;
    private final String titulo;
    private final boolean delMarco;

    CodigoError(int estado, String titulo, boolean delMarco) {
        this.estado = estado;
        this.titulo = titulo;
        this.delMarco = delMarco;
    }

    public int estado() {
        return estado;
    }

    public String titulo() {
        return titulo;
    }

    /**
     * El código con que se contesta una queja del marco, según su estado HTTP. Si el catálogo no
     * tiene uno para ese estado: un 5xx es un error interno, y cualquier otro, una solicitud que no
     * se pudo leer.
     */
    public static CodigoError delMarco(int estadoHttp) {
        return Arrays.stream(values())
                .filter(c -> c.delMarco && c.estado == estadoHttp)
                .findFirst()
                .orElse(estadoHttp >= 500 ? ERROR_INTERNO : SOLICITUD_MAL_FORMADA);
    }

    /** El punto único donde se arma un error. Nadie más llena un {@code HttpProblem} a mano. */
    public HttpProblem aProblema(String detalle) {
        return completar(HttpProblem.builder(), detalle).build();
    }

    /** Le pone a una queja ya armada (por el marco) el tipo, el título y el código del catálogo. */
    HttpProblem.Builder completar(HttpProblem.Builder problema, String detalle) {
        return problema
                .withType(URI.create(BASE + name().toLowerCase(Locale.ROOT)))
                .withTitle(titulo)
                .withStatus(estado)
                .withDetail(detalle)
                .with(CAMPO, name());
    }
}
