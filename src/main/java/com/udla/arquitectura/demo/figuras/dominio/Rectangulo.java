package com.udla.arquitectura.demo.figuras.dominio;

public class Rectangulo implements Figura {
    private final double base;
    private final double altura;

    public Rectangulo(double base, double altura) {
        this.base = Figura.positivo("base", base);
        this.altura = Figura.positivo("altura", altura);
    }

    @Override
    public double area() {
        return base * altura;
    }

    @Override
    public double perimetro() {
        return 2 * (base + altura);
    }
}
