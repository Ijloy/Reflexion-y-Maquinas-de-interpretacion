package com.udla.arquitectura.demo.repository;

import com.udla.arquitectura.demo.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Supplier;

/**
 * Patron Proxy: implementa la misma interfaz que el repositorio real,
 * se interpone delante de el y registra cada acceso y su duracion.
 * Ni el service ni el repositorio real saben que existe.
 */
@Primary
@Repository
public class ProductoRepositoryProxy implements ProductoRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductoRepositoryProxy.class);

    private final ProductoRepository repositorioReal;

    public ProductoRepositoryProxy(
            @Qualifier("repositorioProductoEnMemoria") ProductoRepository repositorioReal) {
        this.repositorioReal = repositorioReal;
    }

    @Override
    public List<Producto> listarTodos() {
        return medir("listarTodos()", () -> repositorioReal.listarTodos());
    }

    @Override
    public Producto buscarPorId(Long id) {
        return medir("buscarPorId(" + id + ")", () -> repositorioReal.buscarPorId(id));
    }

    private <T> T medir(String operacion, Supplier<T> accion) {
        log.info("[Proxy] {} -> consultando repositorio real", operacion);
        long inicio = System.currentTimeMillis();
        try {
            return accion.get();
        } finally {
            log.info("[Proxy] {} termino en {} ms", operacion, System.currentTimeMillis() - inicio);
        }
    }
}