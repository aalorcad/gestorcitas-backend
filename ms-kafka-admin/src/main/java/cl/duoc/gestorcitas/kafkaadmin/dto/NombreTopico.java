package cl.duoc.gestorcitas.kafkaadmin.dto;

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
 * Nombre válido de tópico (o de grupo) Kafka: obligatorio, máximo 249 caracteres y solo letras, números, '.', '_' y '-'.
 * Son las mismas reglas que aplica Kafka; validarlas aquí permite responder 400 con un mensaje claro.
 */
@NotBlank(message = "es obligatorio")
@Size(max = 249, message = "no puede superar 249 caracteres")
@Pattern(regexp = "[a-zA-Z0-9._-]+", message = "solo admite letras, números, '.', '_' y '-'")
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreTopico {

    String message() default "nombre Kafka inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
