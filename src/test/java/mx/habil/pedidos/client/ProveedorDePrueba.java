package mx.habil.pedidos.client;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Un proveedor de inventario de mentiras, para las pruebas: un servidor HTTP de verdad, el que
 * ya trae el JDK, en {@code 127.0.0.1} y en un puerto libre. No sale a internet ni necesita un
 * contenedor.
 *
 * <p>Se le dice qué contestar a las siguientes llamadas, en orden. Cuando la lista se acaba,
 * contesta 404.
 */
public final class ProveedorDePrueba implements AutoCloseable {

    private record Respuesta(int estado, String cuerpo, Duration demora) {
    }

    private final HttpServer servidor;
    private final Deque<Respuesta> respuestas = new ArrayDeque<>();
    private final List<String> rutas = new ArrayList<>();

    public ProveedorDePrueba() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.createContext("/", llamada -> {
            Respuesta respuesta;
            synchronized (this) {
                rutas.add(llamada.getRequestURI().getPath());
                respuesta = respuestas.isEmpty() ? new Respuesta(404, "", Duration.ZERO) : respuestas.poll();
            }
            try {
                Thread.sleep(respuesta.demora());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            llamada.getResponseHeaders().add("Content-Type", "application/json");
            try (llamada) {
                llamada.sendResponseHeaders(respuesta.estado(), cuerpo.length == 0 ? -1 : cuerpo.length);
                if (cuerpo.length > 0) {
                    llamada.getResponseBody().write(cuerpo);
                }
            } catch (IOException e) {
                // Quien llamó ya se fue (se le acabó el tiempo): no hay a quién contestarle.
            }
        });
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public synchronized ProveedorDePrueba contestar(int estado, String cuerpo) {
        respuestas.add(new Respuesta(estado, cuerpo, Duration.ZERO));
        return this;
    }

    public synchronized ProveedorDePrueba tardar(Duration demora, int estado, String cuerpo) {
        respuestas.add(new Respuesta(estado, cuerpo, demora));
        return this;
    }

    /** Las rutas que le pidieron, en orden. */
    public synchronized List<String> rutas() {
        return List.copyOf(rutas);
    }

    public synchronized void olvidar() {
        respuestas.clear();
        rutas.clear();
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
