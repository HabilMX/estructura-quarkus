package mx.habil.pedidos.observability;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * La bitácora del local: le pone un número a cada visita.
 *
 * <p>Cada petición recibe un identificador ({@code request_id}). Sale en todas las líneas de la
 * bitácora que esa petición produce, en el cuerpo de todo error y regresa al cliente en el
 * encabezado {@code X-Request-ID}. Con ese número, una queja se encuentra en la bitácora sin buscar
 * por nombre ni por correo.
 *
 * <p>Son dos métodos y no una clase que herede de un filtro: Quarkus REST los encuentra por sus
 * anotaciones. El primero corre antes de buscar la ruta ({@code preMatching}), para que hasta un
 * 404 lleve su número.
 */
public class CorrelacionFilter {

    public static final String ENCABEZADO = "X-Request-ID";
    public static final String CLAVE = "request_id";

    // Lo que llega de fuera no se copia a la bitácora sin revisar: solo letras, números y guiones.
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{1,64}");

    /** Al entrar: toma el número que trae la petición, o inventa uno. */
    @ServerRequestFilter(preMatching = true)
    public void alEntrar(ContainerRequestContext peticion) {
        String recibido = peticion.getHeaderString(ENCABEZADO);
        String id = recibido != null && VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();
        MDC.put(CLAVE, id);
        peticion.setProperty(CLAVE, id);
    }

    /** Al salir: devuelve el número en el encabezado y limpia la bitácora para la siguiente visita. */
    @ServerResponseFilter
    public void alSalir(ContainerRequestContext peticion, ContainerResponseContext respuesta) {
        Object id = peticion.getProperty(CLAVE);
        if (id != null) {
            respuesta.getHeaders().putSingle(ENCABEZADO, id);
        }
        MDC.remove(CLAVE);
    }
}
