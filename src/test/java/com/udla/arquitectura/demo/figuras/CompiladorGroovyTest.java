package com.udla.arquitectura.demo.figuras;

import com.udla.arquitectura.demo.figuras.dinamico.CompiladorGroovy;
import com.udla.arquitectura.demo.figuras.dominio.Figura;
import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba unitaria (sin Spring) de las restricciones que aplica SecureASTCustomizer. */
class CompiladorGroovyTest {

    private final CompiladorGroovy compilador = new CompiladorGroovy();

    private static String figuraQueEjecuta(String instruccion) {
        return """
                class Peligrosa implements Figura {
                    Peligrosa(double lado) { %s }
                    double area() { 0 }
                    double perimetro() { 0 }
                }""".formatted(instruccion);
    }

    @Test
    void compilaUnaFiguraValidaConservandoLosNombresDeParametros() throws Exception {
        Class<? extends Figura> clase = compilador.compilar("""
                class Rombo implements Figura {
                    private final double d1, d2
                    Rombo(double d1, double d2) { this.d1 = d1; this.d2 = d2 }
                    double area() { d1 * d2 / 2 }
                    double perimetro() { 2 * Math.sqrt(d1 * d1 + d2 * d2) }
                }""");
        assertThat(clase.getConstructors()[0].getParameters()[0].getName()).isEqualTo("d1");
        assertThat(clase.getConstructors()[0].newInstance(6.0, 8.0)).extracting(f -> ((Figura) f).nombre()).isEqualTo("Rombo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "System.exit(1)",
            "Runtime.getRuntime().exec('calc')",
            "new ProcessBuilder('calc').start()",
            "new File('C:/secreto.txt').text",
            "java.nio.file.Files.readString(java.nio.file.Path.of('x'))",
            "'calc'.execute()",
            "Class.forName('java.lang.Runtime')",
            "this.class.classLoader",
            "new URL('http://ejemplo.com').text",
            "new GroovyShell().evaluate('1')"
    })
    void bloqueaLlamadasPeligrosas(String instruccion) {
        assertThatThrownBy(() -> compilador.compilar(figuraQueEjecuta(instruccion)))
                .isInstanceOf(DefinicionInvalidaException.class);
    }
}
