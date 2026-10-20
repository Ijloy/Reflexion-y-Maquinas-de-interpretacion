package com.udla.arquitectura.demo.figuras.dominio;

public class Circulo implements Figura {
    private final double radio;

    public Circulo(double radio) {
        this.radio = Figura.positivo("radio", radio);
    }

    @Override
    public double area() {
        return Math.PI * radio * radio;
    }

    @Override
    public double perimetro() {
        return 2 * Math.PI * radio;
    }
}
