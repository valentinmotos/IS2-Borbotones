package com.dulcemayor.service;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.entity.CotizacionDolar;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.CotizacionDolarRepository;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CotizacionDolarService {

    private final CotizacionDolarRepository cotizacionRepository;
    private final CotizacionProvider cotizacionProvider;

    public CotizacionDolarService(CotizacionDolarRepository cotizacionRepository,
            CotizacionProvider cotizacionProvider) {
        this.cotizacionRepository = cotizacionRepository;
        this.cotizacionProvider = cotizacionProvider;
    }

    /** Devuelve el valor en caché por una hora o actualiza desde el proveedor al vencer. */
    @Transactional
    @Cacheable("cotizacionDolarVigente")
    public CotizacionDolarResponse buscarVigente() {
        return actualizarDesdeProveedor().map(CotizacionDolarService::aResponse)
                .orElseGet(() -> cotizacionRepository.findFirstByOrderByFechaDesc()
                        .map(CotizacionDolarService::aResponse)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No hay cotizaciones guardadas y el proveedor externo no está disponible.")));
    }

    /** Alta o actualización ejecutada por la tarea programada; conserva el último dato ante fallos. */
    @Transactional
    @CacheEvict(value = "cotizacionDolarVigente", allEntries = true)
    public CotizacionDolarResponse actualizarCotizacion() {
        return actualizarDesdeProveedor().map(CotizacionDolarService::aResponse)
                .orElseGet(() -> cotizacionRepository.findFirstByOrderByFechaDesc()
                        .map(CotizacionDolarService::aResponse)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No hay cotizaciones guardadas y el proveedor externo no está disponible.")));
    }

    private Optional<CotizacionDolar> actualizarDesdeProveedor() {
        return cotizacionProvider.obtenerCotizacionOficial().map(externa -> {
            java.time.LocalDate diaCotizacion = externa.fecha().toLocalDate();
            CotizacionDolar entidad = cotizacionRepository.findByFecha(diaCotizacion)
                    .orElseGet(CotizacionDolar::new);
            entidad.setFecha(externa.fecha());
            entidad.setValorVenta(externa.valorVenta());
            return cotizacionRepository.save(entidad);
        });
    }

    private static CotizacionDolarResponse aResponse(CotizacionDolar cotizacion) {
        return new CotizacionDolarResponse(
                cotizacion.getFecha(), cotizacion.getValorVenta());
    }
}
