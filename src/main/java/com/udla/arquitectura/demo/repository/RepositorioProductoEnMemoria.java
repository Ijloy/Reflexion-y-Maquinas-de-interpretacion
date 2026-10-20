package com.udla.arquitectura.demo.repository;

import com.udla.arquitectura.demo.model.Producto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementacion del repositorio. Simula una base de datos con latencia.
 */
@Repository
public class RepositorioProductoEnMemoria implements ProductoRepository {

    private static final long LATENCIA_SIMULADA_MS = 1500;

    private final Map<Long, Producto> datos = Map.of(
            1L, new Producto(1L, "Teclado mecanico", "Perifericos", 45.90),
            2L, new Producto(2L, "Monitor 27\"", "Pantallas", 189.00),
            3L, new Producto(3L, "Mouse inalambrico", "Perifericos", 19.50),
            4L, new Producto(4L, "Laptop 14\"", "Computadores", 780.00),
            5L, new Producto(5L, "Audifonos USB-C", "Audio", 32.75)
    );

    @Override
    public List<Producto> listarTodos() {
        simularLatencia();
        return datos.values().stream()
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .collect(Collectors.toList());
    }

    @Override
    public Producto buscarPorId(Long id) {
        simularLatencia();
        Producto producto = datos.get(id);
        if (producto == null) {
            throw new IllegalArgumentException("Producto no encontrado: " + id);
        }
        return producto;
    }

    private void simularLatencia() {
        try {
            Thread.sleep(LATENCIA_SIMULADA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
