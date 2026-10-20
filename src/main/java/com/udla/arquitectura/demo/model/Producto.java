package com.udla.arquitectura.demo.model;

/**
 * Modelo simple para la demo de catalogo de productos.
 * A proposito es un record inmutable: una sola responsabilidad (SRP),
 * sin logica de negocio ni de acceso a datos.
 */
public record Producto(Long id, String nombre, String categoria, double precio) {
}
