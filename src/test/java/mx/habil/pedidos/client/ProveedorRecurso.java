package mx.habil.pedidos.client;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Levanta el proveedor de mentiras ANTES que Quarkus y le dice a Quarkus dónde está.
 *
 * <p>Quarkus lee la dirección del proveedor al arrancar, así que no basta con cambiarla dentro de
 * la prueba: hay que dársela antes. Eso hace {@code start()}. Y con {@code inject()} la prueba
 * recibe el mismo proveedor, en el campo marcado con {@code @ElProveedor}, para decirle qué
 * contestar.
 */
public class ProveedorRecurso implements QuarkusTestResourceLifecycleManager {

    /** Marca el campo de la prueba donde va el proveedor. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ElProveedor {
    }

    private ProveedorDePrueba proveedor;

    @Override
    public Map<String, String> start() {
        proveedor = new ProveedorDePrueba();
        return Map.of("quarkus.rest-client.inventario.url", proveedor.url());
    }

    @Override
    public void inject(TestInjector inyector) {
        inyector.injectIntoFields(proveedor,
                new TestInjector.AnnotatedAndMatchesType(ElProveedor.class, ProveedorDePrueba.class));
    }

    @Override
    public void stop() {
        if (proveedor != null) {
            proveedor.close();
        }
    }
}
