package cl.duoc.gestorcitas.analitica.repository;

/** Proyección de una consulta agrupada: clave (tipo, especialidad, médico...) y cantidad. */
public interface ConteoPorClave {

    String getClave();

    Long getTotal();
}
