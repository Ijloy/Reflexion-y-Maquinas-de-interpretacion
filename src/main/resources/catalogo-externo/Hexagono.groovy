// Hexágono regular publicado por el catálogo externo (simulado).
class Hexagono implements Figura {
    private final double lado

    Hexagono(double lado) {
        this.lado = Figura.positivo("lado", lado)
    }

    double area() { 3 * Math.sqrt(3) / 2 * lado * lado }

    double perimetro() { 6 * lado }
}
