package com.udla.arquitectura.demo.figuras.reflexion;

import com.udla.arquitectura.demo.figuras.excepciones.FiguraNoEncontradaException;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Convierte lo que escribe el cliente ("círculo", "Pentágono") en un nombre de clase
 * válido ("Circulo", "Pentagono"). Además impide que llegue a Class.forName cualquier
 * texto que no sea un identificador simple (sin puntos, barras ni paquetes).
 */
public final class NombreFigura {

    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}");

    private NombreFigura() {
    }

    public static String normalizar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new FiguraNoEncontradaException("Debe indicar el nombre de la figura");
        }
        String sinTildes = DIACRITICOS.matcher(Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)).replaceAll("");
        String normalizado = Character.toUpperCase(sinTildes.charAt(0)) + sinTildes.substring(1);
        if (!IDENTIFICADOR.matcher(normalizado).matches()) {
            throw new FiguraNoEncontradaException("'" + nombre + "' no es un nombre de figura válido");
        }
        return normalizado;
    }
}
