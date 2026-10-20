package com.udla.arquitectura.demo.figuras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pruebas de integración de punta a punta: servidor real en un puerto aleatorio y llamadas HTTP.
 * Cada prueba usa figuras distintas para no depender del orden de ejecución
 * (el registro de figuras dinámicas se comparte en todo el contexto).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SuppressWarnings({"rawtypes", "unchecked"})
class FigurasApiTest {

    @Autowired
    TestRestTemplate http;

    private ResponseEntity<Map> calcular(String figura, Map<String, Object> parametros) {
        return http.postForEntity("/api/figuras/calcular", Map.of("figura", figura, "parametros", parametros), Map.class);
    }

    private ResponseEntity<Map> registrar(String codigo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.TEXT_PLAIN);
        return http.postForEntity("/api/figuras/definiciones", new HttpEntity<>(codigo, cabeceras), Map.class);
    }

    // ---------- figuras nativas ----------

    @Test
    void circuloRadio5PorReflexion() {
        ResponseEntity<Map> r = calcular("Circulo", Map.of("radio", 5));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("figura")).isEqualTo("Circulo");
        assertThat(r.getBody().get("origen")).isEqualTo("NATIVA");
        assertThat(r.getBody().get("parametros")).isEqualTo(Map.of("radio", 5.0));
        assertThat((Double) r.getBody().get("area")).isCloseTo(78.5398, within(1e-4));
        assertThat((Double) r.getBody().get("perimetro")).isCloseTo(31.4159, within(1e-4));
    }

    @Test
    void nombreIgnoraMayusculasYTildes() {
        assertThat(calcular("CÍRCULO", Map.of("radio", 1)).getBody().get("figura")).isEqualTo("Circulo");
        assertThat(calcular("cuadrado", Map.of("lado", 3)).getBody().get("area")).isEqualTo(9.0);
    }

    @Test
    void calculoPorQueryString() {
        ResponseEntity<Map> r = http.getForEntity("/api/figuras/Rectangulo/calcular?base=4&altura=3", Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("area")).isEqualTo(12.0);
        assertThat(r.getBody().get("perimetro")).isEqualTo(14.0);
    }

    @Test
    void trianguloConFormulaDeHeron() {
        ResponseEntity<Map> r = calcular("Triangulo", Map.of("a", 3, "b", 4, "c", 5));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Double) r.getBody().get("area")).isCloseTo(6.0, within(1e-9));
        assertThat(r.getBody().get("perimetro")).isEqualTo(12.0);
    }

    @Test
    void listaFigurasNativasConSusParametros() {
        ResponseEntity<List> r = http.getForEntity("/api/figuras", List.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map>) r.getBody()).contains(
                Map.of("nombre", "Circulo", "origen", "NATIVA", "parametros", List.of("radio")),
                Map.of("nombre", "Rectangulo", "origen", "NATIVA", "parametros", List.of("base", "altura")),
                Map.of("nombre", "Triangulo", "origen", "NATIVA", "parametros", List.of("a", "b", "c")));
    }

    // ---------- figuras dinámicas (Groovy) ----------

    @Test
    void pentagonoGroovyFuncionaComoNativo() throws Exception {
        assertThat(calcular("Pentagono", Map.of("lado", 2)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map> registro = registrar(Files.readString(Path.of("ejemplos/Pentagono.groovy")));
        assertThat(registro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registro.getBody()).isEqualTo(Map.of("nombre", "Pentagono", "origen", "DINAMICA", "parametros", List.of("lado")));

        // mismos endpoints, mismo flujo de reflexión que una nativa
        ResponseEntity<Map> r = http.getForEntity("/api/figuras/Pentágono/calcular?lado=2", Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("figura")).isEqualTo("Pentagono");
        assertThat(r.getBody().get("origen")).isEqualTo("DINAMICA");
        assertThat((Double) r.getBody().get("area")).isCloseTo(6.8819, within(1e-4));
        assertThat(r.getBody().get("perimetro")).isEqualTo(10.0);

        // y valida sus parámetros igual que una nativa
        assertThat(calcular("Pentagono", Map.of("lado", 0)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- catálogo externo ----------

    @Test
    void hexagonoDesconocidoSeImportaDelCatalogoExterno() {
        ResponseEntity<Map> r = calcular("Hexagono", Map.of("lado", 2));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("origen")).isEqualTo("EXTERNA");
        assertThat((Double) r.getBody().get("area")).isCloseTo(10.3923, within(1e-4));
        assertThat(r.getBody().get("perimetro")).isEqualTo(12.0);
    }

    @Test
    void importacionExplicitaDeElipse() {
        ResponseEntity<Map> r = http.postForEntity("/api/figuras/importar/Elipse", null, Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).isEqualTo(Map.of("nombre", "Elipse", "origen", "EXTERNA", "parametros", List.of("a", "b")));

        ResponseEntity<Map> calculo = calcular("Elipse", Map.of("a", 3, "b", 2));
        assertThat((Double) calculo.getBody().get("area")).isCloseTo(18.8496, within(1e-4));
    }

    @Test
    void importarFiguraQueElCatalogoNoTieneDa404() {
        assertThat(http.postForEntity("/api/figuras/importar/Octogono", null, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- errores ----------

    @Test
    void figuraInexistenteDa404() {
        ResponseEntity<Map> r = calcular("Dodecaedro", Map.of("lado", 1));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().get("title")).isEqualTo("Figura no encontrada");
        assertThat(calcular("java.lang.Runtime", Map.of()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void parametrosInvalidosDan400() {
        assertThat(calcular("Circulo", Map.of("lado", 5)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(calcular("Circulo", Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(calcular("Circulo", Map.of("radio", -1)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(calcular("Circulo", Map.of("radio", "abc")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(calcular("Triangulo", Map.of("a", 1, "b", 2, "c", 10)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity("/api/figuras/Cuadrado/calcular?lado=abc", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void codigoGroovyInvalidoDa422() {
        assertThat(registrar("class Roto implements Figura { double area( }").getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(registrar("class NoFigura { double area() { 1 } }").getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(registrar("""
                class Malo implements Figura {
                    Malo(double lado) { System.exit(1) }
                    double area() { 0 }
                    double perimetro() { 0 }
                }""").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        // no puede reemplazar una figura nativa
        assertThat(registrar("class Circulo implements Figura { double area() { 0 }\n double perimetro() { 0 } }")
                .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
