package com.udla.arquitectura.demo.figuras.api;

import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import com.udla.arquitectura.demo.figuras.excepciones.FiguraNoEncontradaException;
import com.udla.arquitectura.demo.figuras.excepciones.ParametrosInvalidosException;
import com.udla.arquitectura.demo.figuras.excepciones.ServicioExternoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del módulo a respuestas HTTP (RFC 7807). Solo aplica a los controladores de figuras. */
@RestControllerAdvice(basePackageClasses = FiguraController.class)
public class ManejadorErrores {

    @ExceptionHandler(FiguraNoEncontradaException.class)
    public ProblemDetail noEncontrada(FiguraNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Figura no encontrada", e.getMessage());
    }

    @ExceptionHandler(ParametrosInvalidosException.class)
    public ProblemDetail parametros(ParametrosInvalidosException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetros inválidos", e.getMessage());
    }

    /** JSON mal formado o un parámetro que no es número ({"radio": "abc"}). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetros inválidos",
                "El cuerpo debe ser {\"figura\": \"...\", \"parametros\": {\"nombre\": número}}");
    }

    @ExceptionHandler(DefinicionInvalidaException.class)
    public ProblemDetail definicion(DefinicionInvalidaException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Definición de figura inválida", e.getMessage());
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail externo(ServicioExternoException e) {
        return problema(HttpStatus.BAD_GATEWAY, "Error del catálogo externo", e.getMessage());
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
