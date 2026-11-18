package mx.habil.pedidos.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.habil.pedidos.client.InventarioClient;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import mx.habil.pedidos.mapper.PedidoMapper;
import mx.habil.pedidos.model.request.CrearPedidoRequest;
import mx.habil.pedidos.model.response.PedidoResponse;
import mx.habil.pedidos.service.PedidoService;
import mx.habil.pedidos.model.vo.Correo;
import mx.habil.pedidos.model.vo.Dinero;
import mx.habil.pedidos.model.vo.EstadoPedido;
import mx.habil.pedidos.model.vo.LineaCotizada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Del jefe de cocina se prueba una sola cosa: a quién llama, en qué orden y con qué. */
@ExtendWith(MockitoExtension.class)
class PedidoFacadeTest {

    @Mock
    private InventarioClient inventario;

    @Mock
    private PedidoService pedidos;

    private PedidoFacade facade;

    @BeforeEach
    void preparar() {
        facade = new PedidoFacade(inventario, pedidos, new PedidoMapper());
    }

    @Test
    void primeroPreguntaAlProveedorYLuegoLePasaAlCocineroLoQueContesto() {
        CrearPedidoRequest comanda = new CrearPedidoRequest("Ana@Tienda.MX", List.of(
                new CrearPedidoRequest.Linea("CAFE-250", 2), new CrearPedidoRequest.Linea("TAZA-01", 1)));
        when(inventario.consultar("CAFE-250"))
                .thenReturn(Optional.of(new InventarioClient.Producto("CAFE-250", 40, 18950L)));
        when(inventario.consultar("TAZA-01"))
                .thenReturn(Optional.of(new InventarioClient.Producto("TAZA-01", 3, 4500L)));
        UUID id = UUID.randomUUID();
        Instant creado = Instant.parse("2026-11-17T18:00:00Z");
        List<LineaCotizada> cotizadas = List.of(
                new LineaCotizada("CAFE-250", 2, 40, new Dinero(18950)),
                new LineaCotizada("TAZA-01", 1, 3, new Dinero(4500)));
        when(pedidos.crear(new Correo("ana@tienda.mx"), cotizadas))
                .thenReturn(new PedidoEntity(id, "ana@tienda.mx", 42400, EstadoPedido.RECIBIDO, creado,
                        List.of(new LineaEntity("CAFE-250", 2, 18950), new LineaEntity("TAZA-01", 1, 4500))));

        PedidoResponse ticket = facade.crear(comanda);

        assertThat(ticket.id()).isEqualTo(id);
        assertThat(ticket.totalCentavos()).isEqualTo(42400);
        InOrder orden = inOrder(inventario, pedidos);
        orden.verify(inventario).consultar("CAFE-250");
        orden.verify(inventario).consultar("TAZA-01");
        orden.verify(pedidos).crear(new Correo("ana@tienda.mx"), cotizadas);
    }

    @Test
    void siElProveedorNoConoceElProductoElCocineroNiSeEntera() {
        CrearPedidoRequest comanda = new CrearPedidoRequest("ana@tienda.mx",
                List.of(new CrearPedidoRequest.Linea("NO-EXISTE", 1)));
        when(inventario.consultar("NO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.crear(comanda))
                .isInstanceOfSatisfying(PedidosException.class, queja -> {
                    assertThat(queja.codigo()).isEqualTo(CodigoError.PRODUCTO_NO_EXISTE);
                    assertThat(queja.getMessage()).isEqualTo("No hay un producto con el sku NO-EXISTE");
                });
        verifyNoInteractions(pedidos);
    }

    @Test
    void siElProveedorNoContestaLaQuejaSubeTalCual() {
        CrearPedidoRequest comanda = new CrearPedidoRequest("ana@tienda.mx",
                List.of(new CrearPedidoRequest.Linea("CAFE-250", 1)));
        PedidosException sinRespuesta = new PedidosException(CodigoError.PROVEEDOR_NO_RESPONDE, "sin respuesta");
        when(inventario.consultar(any())).thenThrow(sinRespuesta);

        assertThatThrownBy(() -> facade.crear(comanda)).isSameAs(sinRespuesta);
        verifyNoInteractions(pedidos);
    }
}
