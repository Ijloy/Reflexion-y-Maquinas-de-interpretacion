package com.udla.arquitectura.demo.repository;

import com.udla.arquitectura.demo.model.Producto;

import java.util.List;

/**
 * Abstraccion para acceder a productos.
 * El service depende de esta interfaz, nunca de la implementacion concreta.
 */
public interface ProductoRepository {

    List<Producto> listarTodos();

    Producto buscarPorId(Long id);
}
