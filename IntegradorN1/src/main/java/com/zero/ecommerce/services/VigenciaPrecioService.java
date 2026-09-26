package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;

@Service
@Transactional(readOnly = true)
public class VigenciaPrecioService {

    private final VigenciaPrecioRepository repository;
    private final ProductoRepository productoRepository;

    public VigenciaPrecioService(VigenciaPrecioRepository repository, ProductoRepository productoRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public VigenciaPrecio crearVigenciaPrecio(String idProducto, LocalDate fechaDesde, double precio)
            throws ErrorServiceException {
        validarFechaYPrecio(fechaDesde, precio);
        validarProducto(idProducto);

        Producto producto = productoRepository.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no existe o fue eliminado."));

        List<VigenciaPrecio> vigencias = repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(idProducto);
        for (VigenciaPrecio vigencia : vigencias) {
            if (vigencia.getFechaHasta() == null) {
                vigencia.setFechaHasta(fechaDesde.minusDays(1));
                repository.save(vigencia);
                break;
            }
        }

        VigenciaPrecio nueva = new VigenciaPrecio();
        nueva.setProducto(producto);
        nueva.setFechaDesde(fechaDesde);
        nueva.setFechaHasta(null);
        nueva.setPrecio(precio);
        return repository.save(nueva);
    }

    public VigenciaPrecio buscarVigenciaVigente(String idProducto) throws ErrorServiceException {
        validarProducto(idProducto);
        return repository.findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no tiene un precio vigente."));
    }

    public List<VigenciaPrecio> listarVigencias(String idProducto) throws ErrorServiceException {
        validarProducto(idProducto);
        return repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(idProducto).stream()
                .sorted(Comparator.comparing(VigenciaPrecio::getFechaDesde).reversed())
                .toList();
    }

    public double buscarPrecioVigente(String idProducto) throws ErrorServiceException {
        return buscarVigenciaVigente(idProducto).getPrecio();
    }

    private void validarFechaYPrecio(LocalDate fechaDesde, double precio) throws ErrorServiceException {
        if (precio <= 0) {
            throw new ErrorServiceException("El precio de la vigencia debe ser mayor a 0.");
        }
        if (fechaDesde == null || fechaDesde.isBefore(LocalDate.now())) {
            throw new ErrorServiceException("La fecha desde de la vigencia no puede ser anterior a hoy.");
        }
    }

    private void validarProducto(String idProducto) throws ErrorServiceException {
        if (idProducto == null || idProducto.isBlank()) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
        if (!productoRepository.existsByIdAndEliminadoFalse(idProducto)) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
    }
}
