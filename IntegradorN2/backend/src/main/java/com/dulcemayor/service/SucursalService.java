package com.dulcemayor.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.dto.SucursalResponse;
import com.dulcemayor.dto.mapper.SucursalMapper;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Imagen;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.entity.Sucursal;
import com.dulcemayor.entity.enums.TipoEmpresa;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DireccionRepository;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MayoristaRepository;
import com.dulcemayor.repository.SucursalRepository;

@Service
@Transactional(readOnly = true)
public class SucursalService extends AbstractCrudService<Sucursal, SucursalRequest, SucursalResponse> {

    private final SucursalRepository sucursalRepository;
    private final MayoristaRepository mayoristaRepository;
    private final LocalidadRepository localidadRepository;
    private final DireccionRepository direccionRepository;
    private final ImagenService imagenService;

    public SucursalService(SucursalRepository sucursalRepository, SucursalMapper mapper,
            MayoristaRepository mayoristaRepository, LocalidadRepository localidadRepository,
            DireccionRepository direccionRepository, ImagenService imagenService) {
        super(sucursalRepository, mapper);
        this.sucursalRepository = sucursalRepository;
        this.mayoristaRepository = mayoristaRepository;
        this.localidadRepository = localidadRepository;
        this.direccionRepository = direccionRepository;
        this.imagenService = imagenService;
    }

    @Transactional public SucursalResponse crearSucursal(SucursalRequest request) { return crear(request); }
    @Transactional public SucursalResponse modificarSucursal(String id, SucursalRequest request) { return modificar(id, request); }
    @Transactional public void eliminarSucursal(String id) { eliminar(id); }
    public SucursalResponse buscarSucursal(String id) { return buscar(id); }
    public List<SucursalResponse> listarSucursalActivo() { return listarActivos(); }

    public List<SucursalResponse> listarSucursalActivo(String idMayorista) {
        buscarMayorista(idMayorista);
        return mapper.aRespuestas(sucursalRepository.findByMayorista_IdAndEliminadoFalseOrderByNombreAsc(idMayorista));
    }

    @Override
    protected void validar(SucursalRequest request, String idActual) {
        if (request == null) throw new ErrorServiceException("Los datos de la sucursal son obligatorios.");
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new ErrorServiceException("El nombre de la sucursal es obligatorio.", "nombre");
        }
        if (request.tipo() == null) throw new ErrorServiceException("El tipo de empresa es obligatorio.", "tipo");
        Mayorista mayorista = buscarMayorista(request.idMayorista());
        if (request.tipo() == TipoEmpresa.SEDE_CENTRAL && sucursalRepository.findByEliminadoFalse().stream()
                .anyMatch(s -> !s.getId().equals(idActual) && s.getMayorista().getId().equals(mayorista.getId())
                        && s.getTipo() == TipoEmpresa.SEDE_CENTRAL)) {
            throw new DuplicadoException("La empresa ya tiene una sede central activa.", "tipo");
        }
        if (request.nombre() != null && !request.nombre().isBlank() && sucursalRepository.findByEliminadoFalse().stream()
                .anyMatch(s -> !s.getId().equals(idActual) && s.getMayorista().getId().equals(mayorista.getId())
                        && com.dulcemayor.utils.TextoUtils.mismoNombre(s.getNombre(), request.nombre()))) {
            throw new DuplicadoException("Ya existe una sucursal con ese nombre para la empresa.", "nombre");
        }
        buscarLocalidad(request.idLocalidad());
        if (request.idImagen() == null || request.idImagen().isBlank()) {
            throw new ErrorServiceException("La foto de la sucursal es obligatoria.", "idImagen");
        }
        Imagen imagen = imagenService.buscarEntidad(request.idImagen());
        if (imagen.getTipoImagen() != TipoImagen.SUCURSAL) {
            throw new ErrorServiceException("La imagen seleccionada no corresponde a una sucursal.", "idImagen");
        }
    }

    @Override protected Sucursal nuevaEntidad() { return new Sucursal(); }

    @Override
    protected void asignar(Sucursal sucursal, SucursalRequest request) {
        super.asignar(sucursal, request);
        Mayorista mayorista = buscarMayorista(request.idMayorista());
        Localidad localidad = buscarLocalidad(request.idLocalidad());
        Direccion direccion = sucursal.getDireccion();
        if (direccion == null) direccion = new Direccion();
        direccion.setCalle(request.calle().strip());
        direccion.setNumeracion(request.numeracion().strip());
        direccion.setBarrio(limpiar(request.barrio()));
        direccion.setManzanaPiso(limpiar(request.manzanaPiso()));
        direccion.setCasaDepartamento(limpiar(request.casaDepartamento()));
        direccion.setReferencia(limpiar(request.referencia()));
        direccion.setLatitud(request.latitud());
        direccion.setLongitud(request.longitud());
        direccion.setLocalidad(localidad);
        sucursal.setDireccion(direccionRepository.save(direccion));
        sucursal.setMayorista(mayorista);
        sucursal.setImagen(imagenService.buscarEntidad(request.idImagen()));
    }

    private String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }

    private Mayorista buscarMayorista(String id) {
        if (id == null || id.isBlank()) throw new ErrorServiceException("La empresa es obligatoria.", "idMayorista");
        return mayoristaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la empresa."));
    }

    private Localidad buscarLocalidad(String id) {
        if (id == null || id.isBlank()) throw new ErrorServiceException("La localidad es obligatoria.", "idLocalidad");
        return localidadRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad."));
    }

    @Override protected String mensajeNoEncontrado() { return "No se encontró la sucursal."; }
    @Override protected Sort orden() { return Sort.by("nombre"); }
}
