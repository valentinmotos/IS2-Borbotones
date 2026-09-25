package com.zero.ecommerce.services;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.LocalidadRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class LocalidadService {

    // Acepta el formato de 4 dígitos (5500) y el CPA de 8 caracteres (M5500ABC).
    private static final Pattern CODIGO_POSTAL = Pattern.compile("\\d{4}|[A-Za-z]\\d{4}[A-Za-z]{3}");

    private final LocalidadRepository repository;
    private final DepartamentoService departamentoService;

    public LocalidadService(LocalidadRepository repository, DepartamentoService departamentoService) {
        this.repository = repository;
        this.departamentoService = departamentoService;
    }

    // Devuelve la localidad creada (el diagrama dice void), igual que los otros services de ubicación.
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Localidad crearLocalidad(String nombre, String codigoPostal, String idDepartamento)
            throws ErrorServiceException {
        Departamento departamento = buscarDepartamentoPadre(idDepartamento);
        validar(nombre, codigoPostal, departamento);
        Localidad localidad = new Localidad();
        asignar(localidad, nombre, codigoPostal, departamento);
        return repository.save(localidad);
    }

    public void validar(String nombre, String codigoPostal, Departamento departamento)
            throws ErrorServiceException {
        validar(nombre, codigoPostal, departamento, null);
    }

    private void validar(String nombre, String codigoPostal, Departamento departamento, String idActual)
            throws ErrorServiceException {
        validarNombre(nombre);
        validarCodigoPostal(codigoPostal);
        if (departamento == null) {
            throw new ErrorServiceException("El departamento es obligatorio.");
        }
        Optional<Localidad> duplicada = repository
                .findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(departamento.getId()).stream()
                .filter(l -> TextoUtils.mismoNombre(l.getNombre(), nombre))
                .findFirst();
        if (duplicada.isPresent() && !duplicada.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una localidad con ese nombre en el departamento seleccionado.");
        }
    }

    private void validarNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la localidad es obligatorio.");
        }
    }

    private void validarCodigoPostal(String codigoPostal) throws ErrorServiceException {
        if (codigoPostal == null || codigoPostal.isBlank()) {
            throw new ErrorServiceException("El código postal de la localidad es obligatorio.");
        }
        if (!CODIGO_POSTAL.matcher(codigoPostal.strip()).matches()) {
            throw new ErrorServiceException(
                    "El código postal debe tener 4 dígitos (5500) o el formato CPA (M5500ABC).");
        }
    }

    private Departamento buscarDepartamentoPadre(String idDepartamento) throws ErrorServiceException {
        if (idDepartamento == null || idDepartamento.isBlank()) {
            throw new ErrorServiceException("El departamento es obligatorio.");
        }
        return departamentoService.buscarDepartamento(idDepartamento);
    }

    private void asignar(Localidad localidad, String nombre, String codigoPostal, Departamento departamento) {
        localidad.setNombre(nombre.strip());
        localidad.setCodigoPostal(codigoPostal.strip().toUpperCase());
        localidad.setDepartamento(departamento);
    }

    public Localidad buscarLocalidad(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La localidad no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La localidad no existe o fue eliminada."));
    }

    public Localidad buscarLocalidadPorNombre(String nombre) throws ErrorServiceException {
        validarNombre(nombre);
        return repository.findByEliminadoFalseOrderByNombreAsc().stream()
                .filter(l -> TextoUtils.mismoNombre(l.getNombre(), nombre))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("La localidad no existe o fue eliminada."));
    }

    /** Varias localidades pueden compartir el código postal: devuelve la primera por nombre. */
    public Localidad buscarLocalidadPorCodigoPostal(String codigoPostal) throws ErrorServiceException {
        if (codigoPostal == null || codigoPostal.isBlank()) {
            throw new ErrorServiceException("El código postal de la localidad es obligatorio.");
        }
        return repository.findByCodigoPostalAndEliminadoFalseOrderByNombreAsc(codigoPostal.strip().toUpperCase())
                .stream().findFirst()
                .orElseThrow(() -> new ErrorServiceException("No hay localidades con ese código postal."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarLocalidad(String id, String nombre, String codigoPostal, String idDepartamento)
            throws ErrorServiceException {
        Localidad localidad = buscarLocalidad(id);
        Departamento departamento = buscarDepartamentoPadre(idDepartamento);
        validar(nombre, codigoPostal, departamento, id);
        asignar(localidad, nombre, codigoPostal, departamento);
        repository.save(localidad);
    }

    // Las direcciones que usan la localidad la siguen referenciando: la baja es lógica.
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarLocalidad(String id) throws ErrorServiceException {
        Localidad localidad = buscarLocalidad(id);
        localidad.setEliminado(true);
        repository.save(localidad);
    }

    /** Localidades de un departamento, incluidas las eliminadas. Sin departamento, devuelve todas. */
    public List<Localidad> listarLocalidad(String idDepartamento) {
        return sinFiltro(idDepartamento) ? repository.findAllByOrderByNombreAsc()
                : repository.findByDepartamento_IdOrderByNombreAsc(idDepartamento);
    }

    /** Localidades activas de un departamento. Sin departamento, devuelve todas las activas. */
    public List<Localidad> listarLocalidadActivo(String idDepartamento) {
        return sinFiltro(idDepartamento) ? repository.findByEliminadoFalseOrderByNombreAsc()
                : repository.findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(idDepartamento);
    }

    /** Filas del listado del ABM: nombre, código postal, departamento y provincia. */
    public List<FilaTablaDTO> listarFilaLocalidadActivo(String idDepartamento) {
        return listarLocalidadActivo(idDepartamento).stream()
                .map(l -> new FilaTablaDTO(l.getId(), l.getNombre(), List.of(l.getNombre(), l.getCodigoPostal(),
                        l.getDepartamento().getNombre(), l.getDepartamento().getProvincia().getNombre())))
                .toList();
    }

    private boolean sinFiltro(String id) {
        return id == null || id.isBlank();
    }
}
