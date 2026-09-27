package cl.duoc.gestorcitas.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

    private SimpleClientHttpRequestFactory factory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(8000);
        return f;
    }
}
