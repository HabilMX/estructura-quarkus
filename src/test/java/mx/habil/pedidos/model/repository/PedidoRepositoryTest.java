package mx.habil.pedidos.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.habil.pedidos.model.entity.LineaEntity;
import mx.habil.pedidos.model.entity.PedidoEntity;
import mx.habil.pedidos.model.vo.EstadoPedido;
import org.junit.jupiter.api.Test;

/**
 * La despensa se prueba contra una base de verdad: H2, en memoria. Cada prueba corre en una
 * transacción que al final se deshace ({@code @TestTransaction}): la siguiente encuentra la base
 * como estaba.
 */
@QuarkusTest
class PedidoRepositoryTest {

    @Inject
    PedidoRepository repository;

    @Test
    @TestTransaction
    void loQueSeGuardaRegresaIgualConSusLineasEnOrden() {
        UUID id = UUID.randomUUID();
        Instant creado = Instant.parse("2026-11-17T18:00:00Z");
        repository.persist(new PedidoEntity(id, "ana@tienda.mx", 42400, EstadoPedido.RECIBIDO, creado,
                List.of(new LineaEntity("CAFE-250", 2, 18950), new LineaEntity("TAZA-01", 1, 4500))));
        // Se manda a la base y se olvida lo que hay en memoria: lo que sigue se lee de las tablas.
        repository.flush();
        repository.getEntityManager().clear();

        PedidoEntity leido = repository.findByIdOptional(id).orElseThrow();

        assertThat(leido.getCorreo()).isEqualTo("ana@tienda.mx");
        assertThat(leido.getTotalCentavos()).isEqualTo(42400);
        assertThat(leido.getEstado()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(leido.getCreadoEn()).isEqualTo(creado);
        assertThat(leido.getLineas()).containsExactly(
                new LineaEntity("CAFE-250", 2, 18950), new LineaEntity("TAZA-01", 1, 4500));
    }

    @Test
    @TestTransaction
    void unIdentificadorQueNoEstaRegresaVacio() {
        assertThat(repository.findByIdOptional(UUID.randomUUID())).isEmpty();
    }
}
