package cl.duoc.gestorcitas.notificaciones;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Levanta el contexto (sin broker) y verifica que la topología declarada está completa. */
@SpringBootTest
class MsNotificacionesApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void declaraTresFlujosConColaReintentoYDlq() {
        // 3 colas principales + 3 de reintento + 3 DLQ
        assertThat(context.getBeansOfType(Queue.class)).hasSize(9);
        // 3 bindings a los exchanges de negocio + 3 bindings de DLQ al dead letter exchange
        assertThat(context.getBeansOfType(Binding.class)).hasSize(6);

        Queue principal = context.getBean("notificacionesCitasQueue", Queue.class);
        assertThat(principal.getArguments())
                .containsEntry("x-dead-letter-exchange", "gestorcitas.dlx")
                .containsEntry("x-dead-letter-routing-key", "q.notificaciones.citas.dlq");

        Queue reintento = context.getBean("notificacionesCitasRetryQueue", Queue.class);
        assertThat(reintento.getArguments())
                .containsEntry("x-dead-letter-routing-key", "q.notificaciones.citas")
                .containsKey("x-message-ttl");
    }
}
