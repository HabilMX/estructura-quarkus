package mx.habil.pedidos.client;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * El teléfono del proveedor: una interfaz, y Quarkus escribe al construir el código que hace la
 * llamada HTTP. La dirección sale de {@code application.yaml}, bajo
 * {@code quarkus.rest-client.inventario}.
 *
 * <p>Aquí también están el tiempo límite y el reintento, con exactitud:
 *
 * <ul>
 *   <li><b>Tiempo límite</b> ({@code @Timeout}): 2 s por intento. Si el proveedor no contesta en
 *       ese tiempo, el intento se da por perdido.
 *   <li><b>Reintento</b> ({@code @Retry}): hasta 2 veces más, o sea 3 intentos en total. Solo
 *       cuando no hubo respuesta: el tiempo se acabó ({@code TimeoutException}) o no se pudo hablar
 *       con él ({@code ProcessingException}). Una respuesta, aunque sea un 404 o un 500, no se
 *       repite: volver a preguntar no cambia lo que contestó.
 *   <li><b>Espera</b> ({@code @ExponentialBackoff}): 200 ms antes del primer reintento y el doble
 *       antes del segundo, cada una con hasta 50 ms de azar, para que varios clientes no vuelvan a
 *       llamar todos al mismo tiempo.
 * </ul>
 *
 * <p>Los números de las anotaciones son los de siempre; {@code application.yaml} los puede
 * cambiar sin recompilar, por ambiente.
 */
@RegisterRestClient(configKey = "inventario")
@Path("/productos")
public interface InventarioApi {

    @GET
    @Path("/{sku}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 50,
            retryOn = {TimeoutException.class, ProcessingException.class})
    @ExponentialBackoff(factor = 2)
    InventarioClient.Producto producto(@PathParam("sku") String sku);
}
