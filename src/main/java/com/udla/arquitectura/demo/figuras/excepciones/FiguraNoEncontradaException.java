package com.udla.arquitectura.demo.figuras.excepciones;

public class FiguraNoEncontradaException extends RuntimeException {
    public FiguraNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public FiguraNoEncontradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
