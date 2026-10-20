package com.udla.arquitectura.demo.figuras.api;

import com.udla.arquitectura.demo.figuras.excepciones.ParametrosInvalidosException;
import com.udla.arquitectura.demo.figuras.servicio.ServicioFiguras;
import com.udla.arquitectura.demo.figuras.servicio.ServicioFiguras.Descriptor;
import com.udla.arquitectura.demo.figuras.servicio.ServicioFiguras.Resultado;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adaptador HTTP: traduce peticiones a llamadas al servicio. Sin lógica de negocio. */
@RestController
@RequestMapping("/api/figuras")
public class FiguraController {

    public record SolicitudCalculo(String figura, Map<String, Double> parametros) {
    }

    private final ServicioFiguras servicio;

    public FiguraController(ServicioFiguras servicio) {
        this.servicio = servicio;
    }

    /** Lista las figuras disponibles (nativas y dinámicas) con sus parámetros. */
    @GetMapping
    public List<Descriptor> listar() {
        return servicio.listar();
    }

    @GetMapping("/{nombre}")
    public Descriptor describir(@PathVariable String nombre) {
        return servicio.describir(nombre);
    }

    /** POST {"figura": "Circulo", "parametros": {"radio": 5}} */
    @PostMapping("/calcular")
    public Resultado calcular(@RequestBody SolicitudCalculo solicitud) {
        return servicio.calcular(solicitud.figura(), solicitud.parametros());
    }

    /** Atajo: GET /api/figuras/Circulo/calcular?radio=5 */
    @GetMapping("/{nombre}/calcular")
    public Resultado calcularPorUrl(@PathVariable String nombre, @RequestParam Map<String, String> parametros) {
        return servicio.calcular(nombre, aNumeros(parametros));
    }

    /** Registra una figura nueva a partir de código Groovy (texto plano). */
    @PostMapping(path = "/definiciones", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Descriptor registrar(@RequestBody String codigo) {
        return servicio.registrar(codigo);
    }

    /** Extra: trae el código desde el catálogo externo, lo compila y lo registra. */
    @PostMapping("/importar/{nombre}")
    @ResponseStatus(HttpStatus.CREATED)
    public Descriptor importar(@PathVariable String nombre) {
        return servicio.importar(nombre);
    }

    private static Map<String, Double> aNumeros(Map<String, String> texto) {
        Map<String, Double> numeros = new LinkedHashMap<>();
        texto.forEach((nombre, valor) -> {
            try {
                numeros.put(nombre, Double.valueOf(valor.trim()));
            } catch (NumberFormatException e) {
                throw new ParametrosInvalidosException("El parámetro '" + nombre + "' debe ser numérico (recibido: " + valor + ")");
            }
        });
        return numeros;
    }
}
