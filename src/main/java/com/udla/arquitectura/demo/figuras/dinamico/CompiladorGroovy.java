package com.udla.arquitectura.demo.figuras.dinamico;

import com.udla.arquitectura.demo.figuras.dominio.Figura;
import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import com.udla.arquitectura.demo.figuras.reflexion.RegistroFiguras;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.util.Eval;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Máquina de interpretación: compila código fuente Groovy a bytecode JVM mientras
 * la aplicación está corriendo. La clase resultante es una clase Java más; por eso
 * una figura como Pentagono se comporta igual que una nativa.
 *
 * Solo compila y valida; no registra ni instancia nada (eso lo hacen RegistroFiguras y FabricaFiguras).
 */
@Component
public class CompiladorGroovy {

    /** Clases que el código recibido no puede usar ni como receptor, ni con new, ni importar. */
    private static final List<Class<?>> CLASES_PROHIBIDAS = List.of(
            System.class, Runtime.class, ProcessBuilder.class, Process.class, Thread.class,
            ClassLoader.class, Class.class, java.io.File.class, java.nio.file.Files.class,
            java.nio.file.Paths.class, java.nio.file.Path.class, java.net.URL.class, java.net.Socket.class,
            GroovyShell.class, GroovyClassLoader.class, Eval.class);

    /** Métodos que permitirían escapar de las restricciones anteriores por otro camino. */
    private static final Set<String> METODOS_PROHIBIDOS = Set.of(
            "exit", "halt", "exec", "execute", "getRuntime", "forName", "getClassLoader",
            "newInstance", "setAccessible", "invokeMethod", "evaluate", "getMetaClass", "setMetaClass");

    /** Propiedades equivalentes a los métodos anteriores (this.class, x.metaClass...). */
    private static final Set<String> PROPIEDADES_PROHIBIDAS = Set.of("class", "classLoader", "metaClass", "runtime");

    private final GroovyClassLoader cargador;

    public CompiladorGroovy() {
        CompilerConfiguration configuracion = new CompilerConfiguration();
        // Conserva los nombres de los parámetros del constructor (lado, radio...) para la reflexión
        configuracion.setParameters(true);

        // El código Groovy puede usar "Figura" sin escribir el import
        ImportCustomizer imports = new ImportCustomizer();
        imports.addImports(Figura.class.getName());

        configuracion.addCompilationCustomizers(imports, seguridad());
        this.cargador = new GroovyClassLoader(Figura.class.getClassLoader(), configuracion);
    }

    /** Compila el código y verifica por reflexión que la clase implemente Figura. */
    public Class<? extends Figura> compilar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DefinicionInvalidaException("El código de la figura está vacío");
        }
        Class<?> clase;
        try {
            clase = cargador.parseClass(codigo);
        } catch (CompilationFailedException e) {
            throw new DefinicionInvalidaException("El código Groovy no es válido: " + resumir(e.getMessage()), e);
        }
        return RegistroFiguras.comoFiguraConcreta(clase);
    }

    /**
     * Restricciones en tiempo de compilación (análisis del AST). Bloquea lo más evidente
     * —System.exit, Runtime, ProcessBuilder, File, red, reflexión—, pero no es un sandbox
     * completo: la defensa real es no aceptar código de fuentes no confiables.
     */
    private static SecureASTCustomizer seguridad() {
        SecureASTCustomizer seguridad = new SecureASTCustomizerDeClases();
        List<String> nombres = CLASES_PROHIBIDAS.stream().map(Class::getName).toList();
        seguridad.setDisallowedReceivers(nombres);
        seguridad.setDisallowedImports(nombres);
        seguridad.setDisallowedStarImports(List.of("java.io.", "java.nio.", "java.nio.file.", "java.net.",
                "java.lang.reflect.", "java.lang.invoke."));
        // También revisa las clases usadas sin import explícito (Groovy importa java.io.* por defecto)
        seguridad.setIndirectImportCheckEnabled(true);
        seguridad.setMethodDefinitionAllowed(true);
        seguridad.addExpressionCheckers(CompiladorGroovy::expresionPermitida);
        return seguridad;
    }

    /**
     * SecureASTCustomizer (Groovy 4) solo revisa el cuerpo del script y sus métodos sueltos,
     * no el interior de las clases declaradas, que es justo donde vive el código de una Figura.
     * Esta subclase aplica el mismo visitor a constructores, métodos e inicializadores de cada clase.
     */
    private static final class SecureASTCustomizerDeClases extends SecureASTCustomizer {
        @Override
        public void call(SourceUnit fuente, GeneratorContext contexto, ClassNode clase) {
            super.call(fuente, contexto, clase);
            GroovyCodeVisitor visitor = createGroovyCodeVisitor();
            List<MethodNode> metodos = new ArrayList<>(clase.getDeclaredConstructors());
            metodos.addAll(clase.getMethods());
            for (MethodNode metodo : metodos) {
                if (metodo.getCode() != null) {
                    metodo.getCode().visit(visitor);
                }
            }
            for (FieldNode campo : clase.getFields()) {
                if (campo.hasInitialExpression()) {
                    campo.getInitialExpression().visit(visitor);
                }
            }
            clase.getObjectInitializerStatements().forEach(s -> s.visit(visitor));
        }
    }

    private static boolean expresionPermitida(Expression expresion) {
        if (expresion instanceof MethodCallExpression llamada) {
            return !METODOS_PROHIBIDOS.contains(llamada.getMethodAsString());
        }
        if (expresion instanceof StaticMethodCallExpression llamada) {
            return !METODOS_PROHIBIDOS.contains(llamada.getMethod()) && !prohibida(llamada.getOwnerType());
        }
        if (expresion instanceof PropertyExpression propiedad) {
            return !PROPIEDADES_PROHIBIDAS.contains(propiedad.getPropertyAsString());
        }
        if (expresion instanceof ConstructorCallExpression constructor) {
            return !prohibida(constructor.getType());
        }
        if (expresion instanceof ClassExpression clase) {
            return !prohibida(clase.getType());
        }
        return true;
    }

    private static boolean prohibida(ClassNode tipo) {
        return CLASES_PROHIBIDAS.stream().anyMatch(c -> c.getName().equals(tipo.getName()));
    }

    /** Deja solo las líneas útiles del mensaje del compilador (sin stack trace ni encabezados). */
    private static String resumir(String mensaje) {
        return mensaje.lines()
                .map(String::strip)
                .filter(l -> !l.isEmpty() && !l.startsWith("at ") && !l.startsWith("java.lang.")
                        && !l.equals("startup failed:") && !l.matches("\\d+ errors?") && !l.equals("^"))
                .distinct()
                .reduce((a, b) -> a + " | " + b)
                .orElse(mensaje);
    }
}
