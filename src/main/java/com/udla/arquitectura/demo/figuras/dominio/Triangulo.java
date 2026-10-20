package com.udla.arquitectura.demo.figuras.dominio;

/** Triángulo definido por sus tres lados; el área se calcula con la fórmula de Herón. */
public class Triangulo implements Figura {
    private final double a;
    private final double b;
    private final double c;

    public Triangulo(double a, double b, double c) {
        this.a = Figura.positivo("a", a);
        this.b = Figura.positivo("b", b);
        this.c = Figura.positivo("c", c);
        if (a + b <= c || a + c <= b || b + c <= a) {
            throw new IllegalArgumentException("Los lados " + a + ", " + b + ", " + c + " no cumplen la desigualdad triangular");
        }
    }

    @Override
    public double area() {
        double s = perimetro() / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    @Override
    public double perimetro() {
        return a + b + c;
    }
}
