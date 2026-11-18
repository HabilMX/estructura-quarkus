package mx.habil.pedidos.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El filtro se prueba solo, con una petición de mentiras: sin servidor y sin Quarkus. */
class CorrelacionFilterTest {

    private final CorrelacionFilter filtro = new CorrelacionFilter();

    @AfterEach
    void limpiar() {
        MDC.remove(CorrelacionFilter.CLAVE);
    }

    private static ContainerRequestContext peticionCon(String encabezado) {
        ContainerRequestContext peticion = mock(ContainerRequestContext.class);
        when(peticion.getHeaderString("X-Request-ID")).thenReturn(encabezado);
        return peticion;
    }

    /** Lo que el filtro dejó guardado en la petición para devolverlo al salir. */
    private static String guardado(ContainerRequestContext peticion) {
        ArgumentCaptor<Object> id = ArgumentCaptor.forClass(Object.class);
        verify(peticion).setProperty(eq(CorrelacionFilter.CLAVE), id.capture());
        return (String) id.getValue();
    }

    @Test
    void respetaElIdentificadorQueTraeLaPeticion() {
        ContainerRequestContext peticion = peticionCon("visita-0042");

        filtro.alEntrar(peticion);

        assertThat(MDC.get(CorrelacionFilter.CLAVE)).isEqualTo("visita-0042");
        assertThat(guardado(peticion)).isEqualTo("visita-0042");
    }

    @Test
    void siNoTraeUnoLoInventa() {
        ContainerRequestContext peticion = peticionCon(null);

        filtro.alEntrar(peticion);

        assertThat((String) MDC.get(CorrelacionFilter.CLAVE)).matches("[0-9a-f-]{36}");
        assertThat(guardado(peticion)).isEqualTo(MDC.get(CorrelacionFilter.CLAVE));
    }

    @Test
    void unIdentificadorConCaracteresRarosNoLlegaALaBitacora() {
        ContainerRequestContext peticion = peticionCon("abc def; rm -rf");

        filtro.alEntrar(peticion);

        assertThat((String) MDC.get(CorrelacionFilter.CLAVE)).matches("[0-9a-f-]{36}");
    }

    @Test
    void alSalirDevuelveElNumeroYLaBitacoraQuedaLimpia() {
        ContainerRequestContext peticion = peticionCon(null);
        when(peticion.getProperty(CorrelacionFilter.CLAVE)).thenReturn("visita-0042");
        ContainerResponseContext respuesta = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> encabezados = new MultivaluedHashMap<>();
        when(respuesta.getHeaders()).thenReturn(encabezados);
        MDC.put(CorrelacionFilter.CLAVE, "visita-0042");

        filtro.alSalir(peticion, respuesta);

        assertThat(encabezados.getFirst("X-Request-ID")).isEqualTo("visita-0042");
        assertThat(MDC.get(CorrelacionFilter.CLAVE)).isNull();
    }
}
