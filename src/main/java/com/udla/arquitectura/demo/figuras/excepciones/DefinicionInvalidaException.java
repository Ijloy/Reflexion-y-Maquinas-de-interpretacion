package com.udla.arquitectura.demo.figuras.excepciones;

public class DefinicionInvalidaException extends RuntimeException {
    public DefinicionInvalidaException(String mensaje) {
        super(mensaje);
    }

    public DefinicionInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
