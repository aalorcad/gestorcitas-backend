package cl.duoc.gestorcitas.analitica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ms-analitica: consumidor Kafka del grupo "analitica".
 * Lee el streaming de citas y de estado de usuarios, lo persiste en Oracle (esquema ANALITICA)
 * y expone indicadores en {@code GET /analitica/**} (publicado por el BFF en /api/analitica).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAnaliticaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAnaliticaApplication.class, args);
    }
}
