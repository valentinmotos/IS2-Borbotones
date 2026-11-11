package com.dulcemayor.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.dto.mapper.PuntoDeVentaMapper;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.PuntoDeVentaRepository;

@Service
@Transactional(readOnly = true)
public class PuntoDeVentaService {
    private final PuntoDeVentaRepository puntoRepository;
    private final PuntoDeVentaMapper mapper;
    private final MinoristaService minoristaService;
    private final PreventorService preventorService;
    private final DireccionService direccionService;
    public PuntoDeVentaService(PuntoDeVentaRepository puntoRepository, PuntoDeVentaMapper mapper,
            MinoristaService minoristaService, PreventorService preventorService, DireccionService direccionService) {
        this.puntoRepository = puntoRepository; this.mapper = mapper; this.minoristaService = minoristaService;
        this.preventorService = preventorService; this.direccionService = direccionService;
    }
    public List<PuntoDeVentaResponse> listar(String departamentoId) {
        var puntos = departamentoId == null || departamentoId.isBlank()
                ? puntoRepository.findByEliminadoFalseOrderByNombreComercioAsc()
                : puntoRepository.findByDireccion_Localidad_Departamento_IdAndEliminadoFalseOrderByNombreComercioAsc(departamentoId);
        return puntos.stream().map(mapper::aRespuesta).toList();
    }
    public PuntoDeVentaResponse buscar(String id) { return mapper.aRespuesta(buscarEntidad(id)); }
    @Transactional
    public PuntoDeVentaResponse crear(PuntoDeVentaRequest request, String usuarioId) {
        if (request == null) throw new ErrorServiceException("Los datos del punto de venta son obligatorios.");
        Preventor preventor = preventorService.buscarPorUsuario(usuarioId);
        var minorista = minoristaService.buscarEntidad(request.minoristaId());
        var direccion = direccionService.crear(request.localidadId(), request.calle(), request.numeracion(),
                request.barrio(), request.manzanaPiso(), request.casaDepartamento(), request.referencia(),
                request.latitud(), request.longitud());
        PuntoDeVenta punto = new PuntoDeVenta(); punto.setNombreComercio(request.nombreComercio().strip());
        punto.setMinorista(minorista); punto.setPreventor(preventor); punto.setDireccion(direccion);
        return mapper.aRespuesta(puntoRepository.save(punto));
    }
    @Transactional
    public PuntoDeVentaResponse crearDemo(String nombre, String minoristaId, String localidadId,
            String calle, String numero, double latitud, double longitud, String preventorId) {
        Preventor preventor = preventorService.buscarEntidad(preventorId);
        var minorista = minoristaService.buscarEntidad(minoristaId);
        var direccion = direccionService.crear(localidadId, calle, numero, "Centro", null, null,
                "Punto de venta de demostración", latitud, longitud);
        PuntoDeVenta punto = new PuntoDeVenta(); punto.setNombreComercio(nombre); punto.setMinorista(minorista);
        punto.setPreventor(preventor); punto.setDireccion(direccion);
        return mapper.aRespuesta(puntoRepository.save(punto));
    }
    private PuntoDeVenta buscarEntidad(String id) {
        if (id == null || id.isBlank()) throw new RecursoNoEncontradoException("No se encontró el punto de venta.");
        return puntoRepository.findById(id).filter(p -> !p.isEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el punto de venta."));
    }
}
