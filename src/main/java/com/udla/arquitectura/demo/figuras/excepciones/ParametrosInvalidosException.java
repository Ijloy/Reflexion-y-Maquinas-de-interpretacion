package com.udla.arquitectura.demo.figuras.excepciones;

public class ParametrosInvalidosException extends RuntimeException {
    public ParametrosInvalidosException(String mensaje) {
        super(mensaje);
    }

    public ParametrosInvalidosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
