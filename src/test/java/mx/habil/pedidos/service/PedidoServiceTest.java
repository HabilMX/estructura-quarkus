package mx.habil.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.exception.CodigoError;
import mx.habil.pedidos.exception.PedidosException;
import mx.habil.pedidos.model.repository.PedidoRepository;
import mx.habil.pedidos.model.vo.Correo;
import mx.habil.pedidos.model.vo.Dinero;
import mx.habil.pedidos.model.vo.EstadoPedido;
import mx.habil.pedidos.model.vo.LineaCotizada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** El cocinero se prueba solo: sin servidor, sin base de datos y sin Quarkus. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T18:00:00Z");

    @Mock
    private PedidoRepository repository;

    private PedidoService service;

    @BeforeEach
    void preparar() {
        service = new PedidoService(repository, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void elTotalEsLaSumaDePrecioPorCantidad() {
        PedidoEntity pedido = service.crear(new Correo("ana@tienda.mx"), List.of(
                new LineaCotizada("CAFE-250", 2, 40, new Dinero(18950)),
                new LineaCotizada("TAZA-01", 1, 3, new Dinero(4500))));

        assertThat(pedido.getTotalCentavos()).isEqualTo(42400);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(pedido.getCorreo()).isEqualTo("ana@tienda.mx");
        assertThat(pedido.getCreadoEn()).isEqualTo(AHORA);
        assertThat(pedido.getId()).isNotNull();
        assertThat(pedido.getLineas()).containsExactly(
                new LineaEntity("CAFE-250", 2, 18950), new LineaEntity("TAZA-01", 1, 4500));
        verify(repository).persist(pedido);
    }

    @Test
    void laHoraSeCortaAlMilisegundoParaQueLaBaseLaGuardeIgual() {
        Instant conNanosegundos = Instant.parse("2026-11-17T18:00:00.123456789Z");
        service = new PedidoService(repository, Clock.fixed(conNanosegundos, ZoneOffset.UTC));
        PedidoEntity pedido = service.crear(new Correo("ana@tienda.mx"),
                List.of(new LineaCotizada("TAZA-01", 1, 3, new Dinero(4500))));

        assertThat(pedido.getCreadoEn()).isEqualTo(Instant.parse("2026-11-17T18:00:00.123Z"));
    }

    @Test
    void aceptaPedirJustoLasPiezasQueHay() {
        PedidoEntity pedido = service.crear(new Correo("ana@tienda.mx"),
                List.of(new LineaCotizada("TAZA-01", 3, 3, new Dinero(4500))));

        assertThat(pedido.getTotalCentavos()).isEqualTo(13500);
    }

    @Test
    void siFaltaUnaPiezaNoSeGuardaNada() {
        List<LineaCotizada> lineas = List.of(
                new LineaCotizada("CAFE-250", 2, 40, new Dinero(18950)),
                new LineaCotizada("TAZA-01", 4, 3, new Dinero(4500)));
        Correo cliente = new Correo("ana@tienda.mx");

        assertThatThrownBy(() -> service.crear(cliente, lineas))
                .isInstanceOfSatisfying(PedidosException.class, queja -> {
                    assertThat(queja.codigo()).isEqualTo(CodigoError.SIN_EXISTENCIAS);
                    assertThat(queja.getMessage()).isEqualTo("De TAZA-01 se pidieron 4 y hay 3");
                });
        verify(repository, never()).persist(any(PedidoEntity.class));
    }

    @Test
    void unPedidoSinLineasNoEsUnPedido() {
        Correo cliente = new Correo("ana@tienda.mx");

        assertThatThrownBy(() -> service.crear(cliente, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).persist(any(PedidoEntity.class));
    }

    @Test
    void buscarEntregaLaFichaGuardada() {
        UUID id = UUID.randomUUID();
        PedidoEntity ficha = new PedidoEntity(id, "ana@tienda.mx", 4500, EstadoPedido.RECIBIDO, AHORA,
                List.of(new LineaEntity("TAZA-01", 1, 4500)));
        when(repository.findByIdOptional(id)).thenReturn(Optional.of(ficha));

        assertThat(service.buscar(id)).isSameAs(ficha);
    }

    @Test
    void buscarUnPedidoQueNoExisteEsUnaQuejaDelCatalogo() {
        UUID id = UUID.fromString("7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11");
        when(repository.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOfSatisfying(PedidosException.class, queja -> {
                    assertThat(queja.codigo()).isEqualTo(CodigoError.PEDIDO_NO_ENCONTRADO);
                    assertThat(queja.getMessage())
                            .isEqualTo("No hay un pedido con el identificador 7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11");
                });
    }
}
