package cl.duoc.gestorcitas.usuarios.util;

import java.util.Locale;

/** Validación y normalización del RUT chileno (módulo 11). Funciones puras. */
public final class RutUtils {

    private RutUtils() {
    }

    /** "12.345.678-5" -> "12345678-5" (o null si el formato no es interpretable). */
    public static String normalizar(String rut) {
        if (rut == null) return null;
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (limpio.length() < 2) return null;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.matches("\\d{7,8}")) return null;
        return cuerpo + "-" + dv;
    }

    public static boolean esValido(String rut) {
        String n = normalizar(rut);
        if (n == null) return false;
        String[] partes = n.split("-");
        return calcularDv(Integer.parseInt(partes[0])) == partes[1].charAt(0);
    }

    static char calcularDv(int cuerpo) {
        int suma = 0;
        int factor = 2;
        while (cuerpo > 0) {
            suma += (cuerpo % 10) * factor;
            cuerpo /= 10;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }
}
