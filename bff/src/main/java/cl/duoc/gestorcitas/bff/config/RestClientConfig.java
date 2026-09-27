package cl.duoc.gestorcitas.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Clientes HTTP hacia los microservicios.
 * Usa java.net.http.HttpClient (JdkClientHttpRequestFactory) porque soporta PATCH;
 * HttpURLConnection (SimpleClientHttpRequestFactory) lo rechaza.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient catalogoRestClient(ServicesProperties services) {
        return RestClient.builder().baseUrl(services.catalogoUrl()).requestFactory(factory()).build();
    }

    @Bean
    public RestClient citasRestClient(ServicesProperties services) {
        return RestClient.builder().baseUrl(services.citasUrl()).requestFactory(factory()).build();
    }

    @Bean
    public RestClient usuariosRestClient(ServicesProperties services) {
        return RestClient.builder().baseUrl(services.usuariosUrl()).requestFactory(factory()).build();
    }

    private JdkClientHttpRequestFactory factory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(httpClient);
        f.setReadTimeout(Duration.ofSeconds(8));
        return f;
    }
}
