package com.dulcemayor.service;

import com.dulcemayor.dto.LocalidadRequest;
import com.dulcemayor.dto.LocalidadResponse;
import com.dulcemayor.dto.mapper.LocalidadMapper;
import com.dulcemayor.entity.Departamento;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DepartamentoRepository;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.utils.TextoUtils;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocalidadService extends AbstractCrudService<Localidad, LocalidadRequest, LocalidadResponse> {

    private final LocalidadRepository localidadRepository;
    private final DepartamentoRepository departamentoRepository;

    public LocalidadService(LocalidadRepository localidadRepository, LocalidadMapper mapper,
            DepartamentoRepository departamentoRepository) {
        super(localidadRepository, mapper);
        this.localidadRepository = localidadRepository;
        this.departamentoRepository = departamentoRepository;
    }

    @Transactional
    public LocalidadResponse crearLocalidad(LocalidadRequest request) {
        return crear(request);
    }

    @Transactional
    public LocalidadResponse modificarLocalidad(String id, LocalidadRequest request) {
        return modificar(id, request);
    }

    @Transactional
    public void eliminarLocalidad(String id) {
        eliminar(id);
    }

    public LocalidadResponse buscarLocalidad(String id) {
        return buscar(id);
    }

    public LocalidadResponse buscarLocalidadPorCodigoPostal(String codigoPostal) {
        String codigo = codigoPostal == null ? "" : codigoPostal.strip();
        Localidad localidad = localidadRepository.findByCodigoPostalAndEliminadoFalse(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad para ese código postal."));
        return mapper.aRespuesta(localidad);
    }

    public List<LocalidadResponse> listarLocalidad(String departamentoId) {
        buscarDepartamentoActivo(departamentoId);
        return localidadRepository.findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(departamentoId).stream()
                .map(mapper::aRespuesta)
                .toList();
    }

    public List<LocalidadResponse> listarLocalidadActivo(String departamentoId) {
        return listarLocalidad(departamentoId);
    }

    @Override
    protected void validar(LocalidadRequest request, String idActual) {
        if (request == null || request.nombre() == null || request.nombre().isBlank()) {
            throw new ErrorServiceException("El nombre de la localidad es obligatorio.", "nombre");
        }
        if (request.codigoPostal() == null || !request.codigoPostal().strip().matches("\\d{4}")) {
            throw new ErrorServiceException("El código postal debe tener 4 dígitos.", "codigoPostal");
        }
        Departamento departamento = buscarDepartamentoActivo(request.departamentoId());
        List<Localidad> delDepartamento = localidadRepository
                .findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(departamento.getId());
        boolean nombreDuplicado = delDepartamento.stream()
                .filter(localidad -> !localidad.getId().equals(idActual))
                .anyMatch(localidad -> TextoUtils.mismoNombre(localidad.getNombre(), request.nombre()));
        if (nombreDuplicado) {
            throw new DuplicadoException("Ya existe una localidad con ese nombre en el departamento.", "nombre");
        }
        boolean codigoDuplicado = localidadRepository.findByCodigoPostalAndEliminadoFalse(request.codigoPostal().strip())
                .filter(localidad -> !localidad.getId().equals(idActual))
                .isPresent();
        if (codigoDuplicado) {
            throw new DuplicadoException("Ya existe una localidad con ese código postal.", "codigoPostal");
        }
    }

    @Override
    protected Localidad nuevaEntidad() {
        return new Localidad();
    }

    @Override
    protected void asignar(Localidad entidad, LocalidadRequest request) {
        entidad.setNombre(request.nombre().strip());
        entidad.setCodigoPostal(request.codigoPostal().strip());
        entidad.setDepartamento(buscarDepartamentoActivo(request.departamentoId()));
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "No se encontró la localidad.";
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombre");
    }

    private Departamento buscarDepartamentoActivo(String id) {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("Debés seleccionar un departamento.", "departamentoId");
        }
        return departamentoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el departamento.", "departamentoId"));
    }
}
