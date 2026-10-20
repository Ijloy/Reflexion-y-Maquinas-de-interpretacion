package com.udla.arquitectura.demo.figuras.reflexion;

import com.udla.arquitectura.demo.figuras.dominio.Figura;
import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import com.udla.arquitectura.demo.figuras.excepciones.FiguraNoEncontradaException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responde a una sola pregunta: "¿qué clase corresponde a este nombre?".
 * Primero busca entre las figuras compiladas en tiempo de ejecución y luego,
 * con Class.forName, entre las nativas del paquete dominio. No crea instancias:
 * eso es trabajo de FabricaFiguras.
 */
@Component
public class RegistroFiguras {

    private static final String PAQUETE_NATIVO = Figura.class.getPackageName();

    private record Entrada(Class<? extends Figura> clase, OrigenFigura origen) {
    }

    /** Clave en minúsculas para que la búsqueda no distinga mayúsculas. */
    private final Map<String, Entrada> dinamicas = new ConcurrentHashMap<>();

    /** Nombres simples de las clases nativas (minúsculas -> nombre real), para tolerar "CIRCULO". */
    private final Map<String, String> nombresNativos = escanearNombresNativos();

    /** Devuelve la clase de la figura o lanza FiguraNoEncontradaException. */
    public Class<? extends Figura> resolver(String nombre) {
        return buscar(nombre).orElseThrow(() -> new FiguraNoEncontradaException("No existe la figura '" + nombre + "'"));
    }

    public boolean existe(String nombre) {
        return buscar(nombre).isPresent();
    }

    /** Registra (o redefine) una figura compilada en tiempo de ejecución. No permite reemplazar una nativa. */
    public void registrar(Class<? extends Figura> clase, OrigenFigura origen) {
        if (origen == OrigenFigura.NATIVA) {
            throw new IllegalArgumentException("Las figuras nativas no se registran: se descubren por reflexión");
        }
        String nombre = clase.getSimpleName();
        if (nombresNativos.containsKey(clave(nombre))) {
            throw new DefinicionInvalidaException("'" + nombre + "' ya es una figura nativa y no puede redefinirse");
        }
        dinamicas.put(clave(nombre), new Entrada(clase, origen));
    }

    public OrigenFigura origenDe(Class<? extends Figura> clase) {
        Entrada entrada = dinamicas.get(clave(clase.getSimpleName()));
        return entrada != null && entrada.clase() == clase ? entrada.origen() : OrigenFigura.NATIVA;
    }

    /** Todas las figuras disponibles, ordenadas por nombre. */
    public Map<String, Class<? extends Figura>> todas() {
        Map<String, Class<? extends Figura>> resultado = new TreeMap<>();
        nombresNativos.values().forEach(n -> nativa(n).ifPresent(c -> resultado.put(n, c)));
        dinamicas.values().forEach(e -> resultado.put(e.clase().getSimpleName(), e.clase()));
        return resultado;
    }

    /** Verifica, por reflexión, que la clase implemente Figura y se pueda instanciar. */
    @SuppressWarnings("unchecked")
    public static Class<? extends Figura> comoFiguraConcreta(Class<?> clase) {
        if (!Figura.class.isAssignableFrom(clase)) {
            throw new DefinicionInvalidaException("La clase '" + clase.getSimpleName() + "' no implementa Figura");
        }
        if (clase.isInterface() || Modifier.isAbstract(clase.getModifiers())) {
            throw new DefinicionInvalidaException("La clase '" + clase.getSimpleName() + "' es abstracta y no puede instanciarse");
        }
        return (Class<? extends Figura>) clase;
    }

    // ---------- búsqueda ----------

    private Optional<Class<? extends Figura>> buscar(String nombre) {
        String normalizado = NombreFigura.normalizar(nombre);
        Entrada dinamica = dinamicas.get(clave(normalizado));
        if (dinamica != null) {
            return Optional.of(dinamica.clase());
        }
        // "Circulo" se encuentra directo; "CIRCULO" se corrige con el nombre real antes de Class.forName
        String nombreReal = nombresNativos.getOrDefault(clave(normalizado), normalizado);
        return nativa(nombreReal);
    }

    /** Reflexión pura: Class.forName("com...dominio." + nombre). */
    private Optional<Class<? extends Figura>> nativa(String nombreSimple) {
        try {
            return Optional.of(comoFiguraConcreta(Class.forName(PAQUETE_NATIVO + "." + nombreSimple)));
        } catch (ClassNotFoundException | NoClassDefFoundError | DefinicionInvalidaException e) {
            return Optional.empty();
        }
    }

    private static String clave(String nombre) {
        return nombre.toLowerCase(Locale.ROOT);
    }

    /** Descubre qué figuras nativas existen, sin una lista escrita a mano. */
    private static Map<String, String> escanearNombresNativos() {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AssignableTypeFilter(Figura.class));
        Map<String, String> nombres = new TreeMap<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(PAQUETE_NATIVO)) {
            String nombreClase = definicion.getBeanClassName();
            String simple = nombreClase.substring(nombreClase.lastIndexOf('.') + 1);
            nombres.put(clave(simple), simple);
        }
        return nombres;
    }
}
