package com.udla.arquitectura.demo.figuras.servicio;

import com.udla.arquitectura.demo.figuras.dinamico.ClienteCatalogoExterno;
import com.udla.arquitectura.demo.figuras.dinamico.CompiladorGroovy;
import com.udla.arquitectura.demo.figuras.dominio.Figura;
import com.udla.arquitectura.demo.figuras.excepciones.DefinicionInvalidaException;
import com.udla.arquitectura.demo.figuras.reflexion.FabricaFiguras;
import com.udla.arquitectura.demo.figuras.reflexion.NombreFigura;
import com.udla.arquitectura.demo.figuras.reflexion.OrigenFigura;
import com.udla.arquitectura.demo.figuras.reflexion.RegistroFiguras;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orquesta los casos de uso (calcular, registrar código Groovy, importar del catálogo externo)
 * combinando las piezas; no contiene lógica de reflexión, compilación ni HTTP.
 */
@Service
public class ServicioFiguras {

    public record Resultado(String figura, OrigenFigura origen, Map<String, Double> parametros,
                            double area, double perimetro) {
    }

    public record Descriptor(String nombre, OrigenFigura origen, List<String> parametros) {
    }

    private final RegistroFiguras registro;
    private final FabricaFiguras fabrica;
    private final CompiladorGroovy compilador;
    private final ClienteCatalogoExterno catalogoExterno;

    public ServicioFiguras(RegistroFiguras registro, FabricaFiguras fabrica,
                           CompiladorGroovy compilador, ClienteCatalogoExterno catalogoExterno) {
        this.registro = registro;
        this.fabrica = fabrica;
        this.compilador = compilador;
        this.catalogoExterno = catalogoExterno;
    }

    public Resultado calcular(String nombre, Map<String, Double> parametros) {
        if (!registro.existe(nombre) && catalogoExterno.importacionAutomatica()) {
            importar(nombre); // figura desconocida: se intenta traer del catálogo externo
        }
        Figura figura = fabrica.crear(nombre, parametros);
        return new Resultado(figura.nombre(), registro.origenDe(figura.getClass()), parametros,
                figura.area(), figura.perimetro());
    }

    public Descriptor registrar(String codigoGroovy) {
        Class<? extends Figura> clase = compilador.compilar(codigoGroovy);
        registro.registrar(clase, OrigenFigura.DINAMICA);
        return describir(clase);
    }

    public Descriptor importar(String nombre) {
        String solicitado = NombreFigura.normalizar(nombre);
        Class<? extends Figura> clase = compilador.compilar(catalogoExterno.descargarCodigo(solicitado));
        if (!clase.getSimpleName().equalsIgnoreCase(solicitado)) {
            throw new DefinicionInvalidaException("El catálogo externo devolvió la clase '" + clase.getSimpleName()
                    + "' al pedir '" + solicitado + "'");
        }
        registro.registrar(clase, OrigenFigura.EXTERNA);
        return describir(clase);
    }

    public List<Descriptor> listar() {
        return registro.todas().values().stream().map(this::describir).toList();
    }

    public Descriptor describir(String nombre) {
        return describir(registro.resolver(nombre));
    }

    private Descriptor describir(Class<? extends Figura> clase) {
        return new Descriptor(clase.getSimpleName(), registro.origenDe(clase), fabrica.parametrosEsperados(clase));
    }
}
