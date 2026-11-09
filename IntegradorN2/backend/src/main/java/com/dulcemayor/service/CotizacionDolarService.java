package com.dulcemayor.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.entity.CotizacionDolar;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.CotizacionDolarRepository;

/**
 * Cotización del dólar (E1-08). Pide la cotización al CotizacionProvider (Adapter de DolarAPI),
 * guarda una por día en CotizacionDolar y la expone con caché de una hora. Si la API falla,
 * devuelve la última guardada.
 *
 * Sin @Transactional a propósito: la llamada HTTP no tiene que retener una conexión de la base;
 * cada operación del repositorio ya es transaccional.
 */
@Service
public class CotizacionDolarService {

    public static final String CACHE = "cotizacionDolarVigente";

    private final CotizacionDolarRepository cotizacionRepository;
    private final CotizacionProvider cotizacionProvider;

    public CotizacionDolarService(CotizacionDolarRepository cotizacionRepository,
            CotizacionProvider cotizacionProvider) {
        this.cotizacionRepository = cotizacionRepository;
        this.cotizacionProvider = cotizacionProvider;
    }

    /** buscarCotizacionVigente() del UML: la vigente, cacheada por una hora. */
    @Cacheable(CACHE)
    public CotizacionDolarResponse buscarCotizacionVigente() {
        return obtenerYGuardar();
    }

    /** La usa la tarea programada: consulta la API y deja el resultado en la caché. */
    @CachePut(CACHE)
    public CotizacionDolarResponse actualizarCotizacion() {
        return obtenerYGuardar();
    }

    /** listarCotizacion() del UML: historial, de la más reciente a la más vieja. */
    public List<CotizacionDolarResponse> listarCotizacion() {
        return cotizacionRepository.findAllByOrderByFechaDesc().stream()
                .map(CotizacionDolarService::aResponse).toList();
    }

    private CotizacionDolarResponse obtenerYGuardar() {
        return registrarDesdeProveedor()
                .or(cotizacionRepository::findFirstByOrderByFechaDesc)
                .map(CotizacionDolarService::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay cotizaciones guardadas y el proveedor externo no está disponible."));
    }

    /** registrarCotizacion del UML: una fila por día; si ya existe la del día, se actualiza el valor. */
    private Optional<CotizacionDolar> registrarDesdeProveedor() {
        return cotizacionProvider.obtenerVigente().map(externa -> {
            CotizacionDolar cotizacion = cotizacionRepository.findByFecha(externa.fecha())
                    .orElseGet(CotizacionDolar::new);
            cotizacion.setFecha(externa.fecha());
            cotizacion.setValorVenta(externa.valorVenta());
            return cotizacionRepository.save(cotizacion);
        });
    }

    private static CotizacionDolarResponse aResponse(CotizacionDolar cotizacion) {
        return new CotizacionDolarResponse(cotizacion.getFecha(), cotizacion.getValorVenta());
    }
}
