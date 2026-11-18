package mx.habil.pedidos.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/**
 * La instalación del local: las piezas que alguien tiene que armar una vez, al abrir.
 *
 * <p>Aquí no hay reglas del negocio ni rutas HTTP. Solo se construyen objetos que otros paquetes
 * reciben ya listos por su constructor. En Quarkus casi todo lo demás se arma solo, a partir de
 * {@code application.yaml}: el cliente del proveedor, su tiempo límite y su reintento.
 */
@ApplicationScoped
public class InstalacionConfig {

    /** El reloj. Las pruebas ponen uno detenido para que la hora no cambie entre corridas. */
    @Produces
    @ApplicationScoped
    Clock reloj() {
        return Clock.systemUTC();
    }
}
