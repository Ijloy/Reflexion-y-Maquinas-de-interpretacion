package com.udla.arquitectura.demo.figuras.reflexion;

import com.udla.arquitectura.demo.figuras.dominio.Figura;
import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import com.udla.arquitectura.demo.figuras.excepciones.ParametrosInvalidosException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Crea instancias de Figura solo con reflexión: no hay ni un "new Circulo(...)" en el código.
 *
 * 1. RegistroFiguras traduce el nombre a una Class (Class.forName o clase compilada desde Groovy).
 * 2. getConstructors() + Parameter.getName() (el código se compila con -parameters)
 *    encuentran el constructor cuyos parámetros se llaman igual que los recibidos:
 *    ("Circulo", radio=5) -> Circulo(double radio).
 * 3. Constructor.newInstance(args) crea el objeto.
 *
 * Una figura Groovy recorre exactamente el mismo camino que una nativa.
 */
@Component
public class FabricaFiguras {

    private final RegistroFiguras registro;

    public FabricaFiguras(RegistroFiguras registro) {
        this.registro = registro;
    }

    public Figura crear(String nombre, Map<String, Double> parametros) {
        Class<? extends Figura> clase = registro.resolver(nombre);
        Map<String, Double> valores = parametros == null ? Map.of() : parametros;
        Constructor<?> constructor = constructorPara(clase, valores.keySet())
                .orElseThrow(() -> new ParametrosInvalidosException(clase.getSimpleName() + " espera los parámetros "
                        + parametrosEsperados(clase) + " pero recibió " + new TreeSet<>(valores.keySet())));
        return instanciar(constructor, valores);
    }

    /** Nombres de los parámetros que necesita la figura, descubiertos por reflexión. */
    public List<String> parametrosEsperados(Class<? extends Figura> clase) {
        return Arrays.stream(clase.getConstructors())
                .filter(this::esUtilizable)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .map(c -> Arrays.stream(c.getParameters()).map(Parameter::getName).toList())
                .orElse(List.of());
    }

    /** Constructor público cuyos nombres de parámetro coinciden exactamente con los recibidos. */
    private Optional<Constructor<?>> constructorPara(Class<?> clase, Set<String> nombres) {
        return Arrays.stream(clase.getConstructors())
                .filter(this::esUtilizable)
                .filter(c -> c.getParameterCount() == nombres.size())
                .filter(c -> Arrays.stream(c.getParameters()).allMatch(p -> nombres.contains(p.getName())))
                .findFirst();
    }

    private Figura instanciar(Constructor<?> constructor, Map<String, Double> valores) {
        Parameter[] parametros = constructor.getParameters();
        Object[] args = new Object[parametros.length];
        for (int i = 0; i < parametros.length; i++) {
            Double valor = valores.get(parametros[i].getName());
            if (valor == null) {
                throw new ParametrosInvalidosException("El parámetro '" + parametros[i].getName() + "' es obligatorio");
            }
            args[i] = valor;
        }
        try {
            return (Figura) constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            // La figura rechazó los valores (radio <= 0, lados que no forman un triángulo...)
            if (e.getCause() instanceof IllegalArgumentException invalido) {
                throw new ParametrosInvalidosException(invalido.getMessage(), invalido);
            }
            throw new DefinicionInvalidaException("El constructor de " + constructor.getDeclaringClass().getSimpleName()
                    + " falló: " + e.getCause(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new DefinicionInvalidaException("No se pudo instanciar " + constructor.getDeclaringClass().getSimpleName(), e);
        }
    }

    /** Solo constructores con nombres de parámetro reales (-parameters) y todos de tipo double. */
    private boolean esUtilizable(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameters())
                .allMatch(p -> p.isNamePresent() && (p.getType() == double.class || p.getType() == Double.class));
    }
}
