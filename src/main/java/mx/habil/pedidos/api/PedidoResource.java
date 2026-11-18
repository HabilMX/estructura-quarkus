package mx.habil.pedidos.api;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import mx.habil.pedidos.facade.PedidoFacade;
import mx.habil.pedidos.mapper.PedidoMapper;
import mx.habil.pedidos.model.request.CrearPedidoRequest;
import mx.habil.pedidos.model.response.PedidoResponse;
import mx.habil.pedidos.service.PedidoService;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * El mesero: recibe la comanda, revisa que venga bien llenada y la pasa a la cocina.
 *
 * <p>Aquí está todo lo que es HTTP: rutas, verbos y códigos de respuesta. No hay ni un
 * {@code if} sobre un dato del pedido. La versión de la API ({@code /v1}) va una sola vez, en la
 * clase.
 *
 * <p>En Quarkus REST esto se llama <i>resource</i>, no <i>controller</i>: las anotaciones son las
 * del estándar de Java para REST (Jakarta REST), {@code @Path}, {@code @GET} y {@code @POST}. Cada
 * método corre en un hilo virtual ({@code @RunOnVirtualThread}): espera a la base y al proveedor
 * sin acaparar un hilo del sistema.
 */
@Path("/v1/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    private final PedidoFacade facade;
    private final PedidoService service;
    private final PedidoMapper mapper;

    public PedidoResource(PedidoFacade facade, PedidoService service, PedidoMapper mapper) {
        this.facade = facade;
        this.service = service;
        this.mapper = mapper;
    }

    /** Crear un pedido interviene a dos (el proveedor y el cocinero): pasa por el jefe de cocina. */
    @POST
    @RunOnVirtualThread
    public RestResponse<PedidoResponse> crear(@Valid @NotNull CrearPedidoRequest comanda) {
        PedidoResponse creado = facade.crear(comanda);
        // La ubicación va tal cual, relativa: Jakarta REST convierte en absoluta la que recibe
        // con created(URI), y la dirección absoluta depende de por dónde entró la petición.
        return RestResponse.ResponseBuilder.<PedidoResponse>create(RestResponse.Status.CREATED)
                .header(HttpHeaders.LOCATION, "/v1/pedidos/" + creado.id())
                .entity(creado)
                .build();
    }

    /** Consultar un pedido lo atiende un solo servicio: el atajo, directo al cocinero. */
    @GET
    @Path("/{id}")
    @RunOnVirtualThread
    public PedidoResponse buscar(@PathParam("id") UUID id) {
        return mapper.aResponse(service.buscar(id));
    }
}
