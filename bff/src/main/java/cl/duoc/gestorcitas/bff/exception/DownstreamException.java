package cl.duoc.gestorcitas.bff.exception;

import org.springframework.http.HttpStatusCode;

/** Error devuelto por un microservicio; se propaga al frontend con el mismo status y cuerpo. */
public class DownstreamException extends RuntimeException {

    private final HttpStatusCode status;
    private final String body;

    public DownstreamException(HttpStatusCode status, String body) {
        super("Error de microservicio: " + status);
        this.status = status;
        this.body = body;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
