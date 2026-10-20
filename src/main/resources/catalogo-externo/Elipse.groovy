// Elipse con semiejes a y b, publicada por el catálogo externo (simulado).
// El perímetro usa la aproximación de Ramanujan.
class Elipse implements Figura {
    private final double a
    private final double b

    Elipse(double a, double b) {
        this.a = Figura.positivo("a", a)
        this.b = Figura.positivo("b", b)
    }

    double area() { Math.PI * a * b }

    double perimetro() { Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b))) }
}
