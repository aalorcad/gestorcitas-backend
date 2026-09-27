package cl.duoc.gestorcitas.citas.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Inyecta en el controller el {@code UsuarioContexto} construido desde las cabeceras del BFF. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioActual {
}
