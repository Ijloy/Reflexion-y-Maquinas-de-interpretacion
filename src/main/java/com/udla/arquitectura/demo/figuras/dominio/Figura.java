package com.udla.arquitectura.demo.figuras.dominio;

/**
 * Contrato común. La aplicación solo conoce esta interfaz: las clases concretas
 * (nativas o compiladas desde Groovy) se descubren e instancian en tiempo de ejecución.
 */
public interface Figura {

    double area();

    double perimetro();

    /** Por defecto, el nombre simple de la clase ("Circulo", "Pentagono"...). */
    default String nombre() {
        return getClass().getSimpleName();
    }

    /**
     * Validación compartida por todas las figuras (también las escritas en Groovy).
     * Lanza IllegalArgumentException, que la fábrica traduce a "parámetros inválidos".
     */
    static double positivo(String parametro, double valor) {
        if (!(valor > 0) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException("El parámetro '" + parametro + "' debe ser un número mayor que 0 (recibido: " + valor + ")");
        }
        return valor;
    }
}
