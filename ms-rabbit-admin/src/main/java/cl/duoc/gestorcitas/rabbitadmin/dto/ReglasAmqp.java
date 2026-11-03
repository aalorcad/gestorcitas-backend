package cl.duoc.gestorcitas.rabbitadmin.dto;

/** Reglas de formato para nombres AMQP, compartidas por todas las validaciones de la API. */
public final class ReglasAmqp {

    /** Largo máximo de un nombre de cola/exchange o routing key en RabbitMQ (shortstr). */
    public static final int LARGO_MAXIMO = 255;

    /**
     * Letras, números, punto, guion, guion bajo y dos puntos; debe comenzar con letra o número.
     * El prefijo "amq." está reservado por RabbitMQ.
     */
    public static final String PATRON_NOMBRE = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._:-]*$";

    public static final String MENSAJE_NOMBRE =
            "solo admite letras, números, '.', '_', ':' y '-', debe comenzar con letra o número y no puede empezar con 'amq.'";

    /** Routing key: como el nombre, pero admite los comodines de topic '*' y '#'. Puede ser vacía. */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._:*#-]*$";

    public static final String MENSAJE_ROUTING_KEY =
            "solo admite letras, números, '.', '_', ':', '-' y los comodines '*' y '#'";

    private ReglasAmqp() {
    }
}
