package cl.duoc.gestorcitas.usuarios.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RutUtilsTest {

    @Test
    void validaRutsCorrectos() {
        assertThat(RutUtils.esValido("11.111.111-1")).isTrue();
        assertThat(RutUtils.esValido("12345678-5")).isTrue();
        assertThat(RutUtils.esValido("10.000.013-k")).isTrue();
    }

    @Test
    void rechazaRutsIncorrectos() {
        assertThat(RutUtils.esValido("12345678-9")).isFalse();
        assertThat(RutUtils.esValido("abc")).isFalse();
        assertThat(RutUtils.esValido(null)).isFalse();
    }

    @Test
    void normaliza() {
        assertThat(RutUtils.normalizar("12.345.678-5")).isEqualTo("12345678-5");
    }
}
