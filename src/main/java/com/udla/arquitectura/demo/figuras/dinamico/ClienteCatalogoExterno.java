package com.udla.arquitectura.demo.figuras.dinamico;

import com.udla.arquitectura.demo.figuras.excepciones.FiguraNoEncontradaException;
import com.udla.arquitectura.demo.figuras.excepciones.ServicioExternoException;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Descarga el código fuente Groovy de una figura desde un servicio externo.
 * Contrato: GET {figuras.catalogo.url}/{nombre} -> 200 text/plain con el código, o 404.
 * Solo transporta texto: compilar y registrar no es responsabilidad de esta clase.
 */
@Component
public class ClienteCatalogoExterno {

    static final String PROPIEDAD_URL = "figuras.catalogo.url";

    private final RestClient http;
    private final Environment entorno;

    public ClienteCatalogoExterno(RestClient.Builder builder, Environment entorno) {
        this.http = builder.build();
        this.entorno = entorno;
    }

    public String descargarCodigo(String nombre) {
        String url = urlBase();
        try {
            String codigo = http.get()
                    .uri(url + "/{nombre}", nombre)
                    .retrieve()
                    .body(String.class);
            if (codigo == null || codigo.isBlank()) {
                throw new ServicioExternoException("El catálogo externo devolvió un código vacío para '" + nombre + "'");
            }
            return codigo;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new FiguraNoEncontradaException("No existe la figura '" + nombre + "' (tampoco en el catálogo externo)");
            }
            throw new ServicioExternoException("El catálogo externo respondió " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo contactar el catálogo externo en " + url, e);
        }
    }

    public boolean importacionAutomatica() {
        return entorno.getProperty("figuras.catalogo.importar-automaticamente", Boolean.class, true);
    }

    /** Se lee en cada llamada para poder usar el puerto real del servidor (local.server.port). */
    private String urlBase() {
        return entorno.getRequiredProperty(PROPIEDAD_URL);
    }
}
