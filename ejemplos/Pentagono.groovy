// Pentágono regular. Se envía en tiempo de ejecución a POST /api/figuras/definiciones
// y desde ese momento la fábrica lo instancia por reflexión igual que a Circulo o Cuadrado:
// ("Pentagono", lado=2) -> constructor Pentagono(double lado).
// "Figura" no necesita import: el compilador lo agrega.
class Pentagono implements Figura {
    private final double lado

    Pentagono(double lado) {
        this.lado = Figura.positivo("lado", lado)
    }

    double area() { Math.sqrt(5 * (5 + 2 * Math.sqrt(5))) / 4 * lado * lado }

    double perimetro() { 5 * lado }
}
