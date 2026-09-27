package cl.duoc.gestorcitas.citas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsCitasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCitasApplication.class, args);
    }
}
