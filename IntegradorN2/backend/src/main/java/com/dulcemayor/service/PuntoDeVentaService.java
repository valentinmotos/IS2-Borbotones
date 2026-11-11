package com.dulcemayor.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.dto.mapper.PuntoDeVentaMapper;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.PuntoDeVentaRepository;

/**
 * E2-06: puntos de venta y geolocalización.
 *
 * El preventor da de alta un comercio de un minorista con su dirección y su ubicación en el
 * mapa; el preventor sale siempre del token. La "zona" es el departamento de la localidad de la
 * dirección (el modelo no tiene otra entidad de zona).
 */
@Service
@Transactional(readOnly = true)
public class PuntoDeVentaService {

    private static final String PUNTO_NO_ENCONTRADO = "No se encontró el punto de venta.";

    private final PuntoDeVentaRepository puntoRepository;
    private final PuntoDeVentaMapper mapper;
    private final MinoristaService minoristaService;
    private final PreventorService preventorService;
    private final DireccionService direccionService;

    public PuntoDeVentaService(PuntoDeVentaRepository puntoRepository, PuntoDeVentaMapper mapper,
            MinoristaService minoristaService, PreventorService preventorService, DireccionService direccionService) {
        this.puntoRepository = puntoRepository;
        this.mapper = mapper;
        this.minoristaService = minoristaService;
        this.preventorService = preventorService;
        this.direccionService = direccionService;
    }

    /** Alta de un punto de venta por el preventor autenticado. */
    @Transactional
    public PuntoDeVentaResponse crearPuntoDeVenta(PuntoDeVentaRequest request, String idUsuarioPreventor) {
        validar(request);
        Preventor preventor = preventorService.buscarPreventorPorUsuario(idUsuarioPreventor);
        Minorista minorista = minoristaService.buscarEntidad(request.minoristaId());
        Direccion direccion = direccionService.crearDireccion(request.calle(), request.numeracion(),
                request.barrio(), request.manzanaPiso(), request.casaDepartamento(), request.referencia(),
                request.latitud(), request.longitud(), request.localidadId());

        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setNombreComercio(request.nombreComercio().strip());
        punto.setMinorista(minorista);
        punto.setPreventor(preventor);
        punto.setDireccion(direccion);
        return mapper.aRespuesta(puntoRepository.save(punto));
    }

    void validar(PuntoDeVentaRequest request) {
        if (request == null) {
            throw new ErrorServiceException("Los datos del punto de venta son obligatorios.");
        }
        if (request.nombreComercio() == null || request.nombreComercio().isBlank()) {
            throw new ErrorServiceException("Ingresá el nombre del comercio.", "nombreComercio");
        }
    }

    public PuntoDeVentaResponse buscarPuntoDeVenta(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException(PUNTO_NO_ENCONTRADO);
        }
        return puntoRepository.findByIdAndEliminadoFalse(id).map(mapper::aRespuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException(PUNTO_NO_ENCONTRADO));
    }

    /** Puntos activos; si llega un departamento, solo los de esa zona. */
    public List<PuntoDeVentaResponse> listarPuntoDeVentaActivo(String idDepartamento) {
        List<PuntoDeVenta> puntos = idDepartamento == null || idDepartamento.isBlank()
                ? puntoRepository.findByEliminadoFalseOrderByNombreComercioAsc()
                : puntoRepository.findByDireccion_Localidad_Departamento_IdAndEliminadoFalseOrderByNombreComercioAsc(
                        idDepartamento);
        return mapper.aRespuestas(puntos);
    }

    /** Para el portal del preventor (E4-02): sus comercios, para elegir uno existente. */
    public List<PuntoDeVentaResponse> listarPuntoDeVentaPorPreventor(String idPreventor) {
        return mapper.aRespuestas(puntoRepository.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(idPreventor));
    }

    /** Para el perfil del minorista (E2-07) y sus pedidos. */
    public List<PuntoDeVentaResponse> listarPuntoDeVentaPorMinorista(String idMinorista) {
        return mapper.aRespuestas(puntoRepository.findByMinorista_IdAndEliminadoFalseOrderByNombreComercioAsc(idMinorista));
    }
}
