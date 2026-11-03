package cl.duoc.gestorcitas.rabbitadmin.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nombre obligatorio y válido de cola o exchange: no vacío, máximo 255 caracteres y formato {@link ReglasAmqp#PATRON_NOMBRE}.
 * Se usa en DTOs y en variables de ruta ({@code /queues/{nombre}}).
 */
@NotBlank(message = "es obligatorio")
@Size(max = ReglasAmqp.LARGO_MAXIMO, message = "no puede superar " + ReglasAmqp.LARGO_MAXIMO + " caracteres")
@Pattern(regexp = ReglasAmqp.PATRON_NOMBRE, message = ReglasAmqp.MENSAJE_NOMBRE)
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreAmqp {

    String message() default "nombre AMQP inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
