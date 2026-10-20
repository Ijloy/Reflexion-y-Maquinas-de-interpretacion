package com.udla.arquitectura.demo.controller;

import com.udla.arquitectura.demo.model.Producto;
import com.udla.arquitectura.demo.service.ProductoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST del catalogo.
 */
@RestController
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/api/productos")
    public List<Producto> listar() {
        return productoService.listarTodos();
    }

    @GetMapping("/api/productos/{id}")
    public Producto buscar(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }
}
