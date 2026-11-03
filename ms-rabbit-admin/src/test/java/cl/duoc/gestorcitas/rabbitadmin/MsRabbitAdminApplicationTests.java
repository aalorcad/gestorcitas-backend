package cl.duoc.gestorcitas.rabbitadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** El contexto levanta sin broker (AMQP y management se conectan de forma perezosa). */
@SpringBootTest
class MsRabbitAdminApplicationTests {

    @Test
    void contextLoads() {
    }
}
