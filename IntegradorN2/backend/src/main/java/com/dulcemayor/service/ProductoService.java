package com.dulcemayor.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.dto.ProductoRequest;
import com.dulcemayor.dto.ProductoResponse;
import com.dulcemayor.dto.mapper.ProductoMapper;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;

@Service
public class ProductoService extends AbstractCrudService<Producto, ProductoRequest, ProductoResponse> {

    private final ProductoRepository productos;
    private final CategoriaService categorias;
    private final ImagenService imagenes;

    public ProductoService(ProductoRepository productos, ProductoMapper mapper, CategoriaService categorias,
            ImagenService imagenes) {
        super(productos, mapper);
        this.productos = productos;
        this.categorias = categorias;
        this.imagenes = imagenes;
    }

    public List<ProductoResponse> buscarActivos(String busqueda, String categoriaId) {
        String texto = busqueda == null ? "" : busqueda.strip();
        String categoria = categoriaId == null ? "" : categoriaId.strip();
        return productos.buscarActivos(texto, categoria);
    }

    @Override
    public List<ProductoResponse> listarActivos() {
        return buscarActivos("", "");
    }

    @Override
    protected void validar(ProductoRequest request, String idActual) {
        String codigo = codigoNormalizado(request);
        if (codigo.length() > 50) {
            throw new ErrorServiceException("El código no puede superar los 50 caracteres.", "codigo");
        }
        if (productos.existsByCodigoIgnoreCaseAndIdNot(codigo, idActual == null ? "" : idActual)) {
            throw codigoDuplicado();
        }
    }

    @Override
    protected Producto nuevaEntidad() {
        return new Producto();
    }

    @Override
    protected void asignar(Producto producto, ProductoRequest request) {
        try {
            producto.setCategoria(categorias.buscarEntidad(request.categoriaId()));
        } catch (RecursoNoEncontradoException ex) {
            throw new ErrorServiceException("Seleccioná una categoría activa.", "categoriaId");
        }
        if (request.imagen() != null && !request.imagen().isEmpty()) {
            try {
                var imagen = imagenes.crear(new ImagenCargaDTO(request.imagen(), TipoImagen.PRODUCTO));
                producto.setImagen(imagenes.buscarEntidad(imagen.id()));
            } catch (ErrorServiceException ex) {
                throw new ErrorServiceException(ex.getMessage(), "imagen");
            }
        }
        if (producto.getImagen() == null) {
            throw new ErrorServiceException("Seleccioná una imagen para el producto.", "imagen");
        }

        super.asignar(producto, request);
        producto.setCodigo(codigoNormalizado(request));
        producto.setNombre(request.nombre().strip());
        producto.setPresentacion(request.presentacion().strip());
        if (request.enOferta() != null) {
            producto.setEnOferta(request.enOferta());
        }
    }

    @Override
    protected void despuesDeGuardar(Producto producto) {
        try {
            productos.flush();
        } catch (DataIntegrityViolationException ex) {
            // La restricción única cubre dos altas simultáneas del mismo código.
            if (ex.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                throw codigoDuplicado();
            }
            throw ex;
        }
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "No se encontró el producto.";
    }

    private static String codigoNormalizado(ProductoRequest request) {
        return request.codigo().strip().toUpperCase(Locale.ROOT);
    }

    private static DuplicadoException codigoDuplicado() {
        return new DuplicadoException("Ya existe un producto con ese código.", "codigo");
    }
}
