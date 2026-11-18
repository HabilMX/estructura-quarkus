package mx.habil.pedidos.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.response.PedidoResponse;
import mx.habil.pedidos.model.vo.EstadoPedido;
import org.junit.jupiter.api.Test;

class PedidoMapperTest {

    private final PedidoMapper mapper = new PedidoMapper();

    @Test
    void copiaCadaCampoDeLaFichaAlTicket() {
        UUID id = UUID.fromString("7b1e0f5e-3c7a-4a51-9d0e-2f6a1c9b8d11");
        Instant creado = Instant.parse("2026-11-17T18:00:00Z");
        PedidoEntity ficha = new PedidoEntity(id, "ana@tienda.mx", 42400, EstadoPedido.RECIBIDO, creado,
                List.of(new LineaEntity("CAFE-250", 2, 18950), new LineaEntity("TAZA-01", 1, 4500)));

        PedidoResponse ticket = mapper.aResponse(ficha);

        assertThat(ticket).isEqualTo(new PedidoResponse(id, "RECIBIDO", 42400, creado,
                List.of(new PedidoResponse.Linea("CAFE-250", 2, 18950), new PedidoResponse.Linea("TAZA-01", 1, 4500))));
    }

    @Test
    void elTicketNoLlevaElCorreo() {
        assertThat(PedidoResponse.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("correo");
    }
}
