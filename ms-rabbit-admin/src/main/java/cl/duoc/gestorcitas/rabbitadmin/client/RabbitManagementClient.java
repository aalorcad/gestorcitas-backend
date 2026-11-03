package cl.duoc.gestorcitas.rabbitadmin.client;

import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementBinding;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementExchange;
import cl.duoc.gestorcitas.rabbitadmin.client.dto.ManagementQueue;
import cl.duoc.gestorcitas.rabbitadmin.config.RabbitManagementProperties;
import cl.duoc.gestorcitas.rabbitadmin.exception.ServicioNoDisponibleException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cliente de solo lectura de la API HTTP de management de RabbitMQ.
 * AMQP no permite listar colas/exchanges/bindings, por eso las consultas usan esta API;
 * las operaciones de creación y eliminación se hacen por AMQP con {@code AmqpAdmin}.
 */
@Component
public class RabbitManagementClient {

    private final RestClient client;
    private final String vhost;

    public RabbitManagementClient(RestClient.Builder builder, RabbitManagementProperties props) {
        this.client = builder
                .baseUrl(props.url())
                .defaultHeaders(h -> h.setBasicAuth(props.username(), props.password()))
                .build();
        this.vhost = props.vhost();
    }

    public List<ManagementQueue> listarColas() {
        return llamar(() -> client.get().uri("/api/queues/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<ManagementQueue>>() { }));
    }

    public List<ManagementExchange> listarExchanges() {
        return llamar(() -> client.get().uri("/api/exchanges/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<ManagementExchange>>() { }));
    }

    public List<ManagementBinding> listarBindings() {
        return llamar(() -> client.get().uri("/api/bindings/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<ManagementBinding>>() { }));
    }

    public Optional<ManagementExchange> buscarExchange(String nombre) {
        try {
            return Optional.ofNullable(llamar(() -> client.get()
                    .uri("/api/exchanges/{vhost}/{nombre}", vhost, nombre)
                    .retrieve().body(ManagementExchange.class)));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    private <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException.NotFound ex) {
            throw ex; // lo interpreta el método que llama
        } catch (ResourceAccessException ex) {
            throw new ServicioNoDisponibleException("La API de management de RabbitMQ no responde", ex);
        } catch (RestClientResponseException ex) {
            throw new ServicioNoDisponibleException("La API de management de RabbitMQ respondió "
                    + ex.getStatusCode().value() + ". Revisa la URL y las credenciales de management.", ex);
        }
    }
}
