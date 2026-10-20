package com.udla.arquitectura.demo.figuras.dominio;

public class Cuadrado implements Figura {
    private final double lado;

    public Cuadrado(double lado) {
        this.lado = Figura.positivo("lado", lado);
    }

    @Override
    public double area() {
        return lado * lado;
    }

    @Override
    public double perimetro() {
        return 4 * lado;
    }
}
