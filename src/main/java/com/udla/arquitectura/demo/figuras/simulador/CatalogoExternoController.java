package com.udla.arquitectura.demo.figuras.simulador;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * SIMULA un servicio externo que publica código Groovy de figuras (Hexagono, Elipse),
 * para poder probar el extra sin otro servidor. La aplicación NO lo usa directamente:
 * lo consume por HTTP con ClienteCatalogoExterno, igual que a un servicio en otra máquina.
 * Para usar un catálogo real basta con cambiar figuras.catalogo.url en application.properties.
 *
 * Contrato: GET /catalogo-externo/{nombre} -> 200 text/plain (código) | 404.
 */
@RestController
@RequestMapping("/catalogo-externo")
public class CatalogoExternoController {

    private static final String CARPETA = "classpath:catalogo-externo/";
    private final PathMatchingResourcePatternResolver recursos = new PathMatchingResourcePatternResolver();

    @GetMapping
    public List<String> listar() throws IOException {
        return Arrays.stream(recursos.getResources(CARPETA + "*.groovy"))
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .map(nombre -> nombre.replace(".groovy", ""))
                .sorted()
                .toList();
    }

    @GetMapping(path = "/{nombre}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> obtener(@PathVariable String nombre) {
        if (!nombre.matches("[A-Za-z0-9]+")) {
            return ResponseEntity.notFound().build();
        }
        Resource archivo = recursos.getResource(CARPETA + nombre + ".groovy");
        if (!archivo.exists()) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok()
                    .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                    .body(StreamUtils.copyToString(archivo.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
