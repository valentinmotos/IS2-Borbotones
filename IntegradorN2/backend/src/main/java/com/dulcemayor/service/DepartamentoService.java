package com.dulcemayor.service;

import com.dulcemayor.dto.DepartamentoRequest;
import com.dulcemayor.dto.DepartamentoResponse;
import com.dulcemayor.dto.mapper.DepartamentoMapper;
import com.dulcemayor.entity.Departamento;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.DepartamentoRepository;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.utils.TextoUtils;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartamentoService extends AbstractCrudService<Departamento, DepartamentoRequest, DepartamentoResponse> {

    private final DepartamentoRepository departamentoRepository;
    private final LocalidadRepository localidadRepository;

    public DepartamentoService(DepartamentoRepository departamentoRepository, DepartamentoMapper mapper,
            LocalidadRepository localidadRepository) {
        super(departamentoRepository, mapper);
        this.departamentoRepository = departamentoRepository;
        this.localidadRepository = localidadRepository;
    }

    @Transactional
    public DepartamentoResponse crearDepartamento(DepartamentoRequest request) {
        return crear(request);
    }

    @Transactional
    public DepartamentoResponse modificarDepartamento(String id, DepartamentoRequest request) {
        return modificar(id, request);
    }

    @Transactional
    public void eliminarDepartamento(String id) {
        eliminar(id);
    }

    public DepartamentoResponse buscarDepartamento(String id) {
        return buscar(id);
    }

    public List<DepartamentoResponse> listarDepartamentoActivo() {
        return listarActivos();
    }

    public void validar(String nombre) {
        validar(new DepartamentoRequest(nombre), null);
    }

    @Override
    protected void validar(DepartamentoRequest request, String idActual) {
        String nombre = request == null ? null : request.nombre();
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre del departamento es obligatorio.", "nombre");
        }
        boolean duplicado = departamentoRepository.findByEliminadoFalse().stream()
                .filter(departamento -> !departamento.getId().equals(idActual))
                .anyMatch(departamento -> TextoUtils.mismoNombre(departamento.getNombre(), nombre));
        if (duplicado) {
            throw new DuplicadoException("Ya existe un departamento con ese nombre.", "nombre");
        }
    }

    @Override
    protected Departamento nuevaEntidad() {
        return new Departamento();
    }

    @Override
    protected void asignar(Departamento entidad, DepartamentoRequest request) {
        entidad.setNombre(request.nombre().strip());
    }

    @Override
    protected void antesDeEliminar(Departamento departamento) {
        if (!localidadRepository.findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(departamento.getId())
                .isEmpty()) {
            throw new ErrorServiceException("No se puede dar de baja el departamento porque tiene localidades activas.");
        }
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "No se encontró el departamento.";
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombre");
    }
}
