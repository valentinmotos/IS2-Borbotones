package com.zero.ecommerce.services;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.DepartamentoRepository;
import com.zero.ecommerce.repositories.LocalidadRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class DepartamentoService {

    private final DepartamentoRepository repository;
    private final LocalidadRepository localidadRepository;
    private final ProvinciaService provinciaService;

    public DepartamentoService(DepartamentoRepository repository, LocalidadRepository localidadRepository,
            ProvinciaService provinciaService) {
        this.repository = repository;
        this.localidadRepository = localidadRepository;
        this.provinciaService = provinciaService;
    }

    // Devuelve el departamento creado (el diagrama dice void) porque el seeder necesita su id.
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Departamento crearDepartamento(String nombre, String idProvincia) throws ErrorServiceException {
        Provincia provincia = buscarProvinciaPadre(idProvincia);
        validar(nombre, provincia);
        Departamento departamento = new Departamento();
        departamento.setNombre(nombre.strip());
        departamento.setProvincia(provincia);
        return repository.save(departamento);
    }

    public void validar(String nombre, Provincia provincia) throws ErrorServiceException {
        validar(nombre, provincia, null);
    }

    private void validar(String nombre, Provincia provincia, String idActual) throws ErrorServiceException {
        validarNombre(nombre);
        if (provincia == null) {
            throw new ErrorServiceException("La provincia es obligatoria.");
        }
        Optional<Departamento> duplicado = repository
                .findByProvincia_IdAndEliminadoFalseOrderByNombreAsc(provincia.getId()).stream()
                .filter(d -> TextoUtils.mismoNombre(d.getNombre(), nombre))
                .findFirst();
        if (duplicado.isPresent() && !duplicado.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe un departamento con ese nombre en la provincia seleccionada.");
        }
    }

    private void validarNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre del departamento es obligatorio.");
        }
    }

    private Provincia buscarProvinciaPadre(String idProvincia) throws ErrorServiceException {
        if (idProvincia == null || idProvincia.isBlank()) {
            throw new ErrorServiceException("La provincia es obligatoria.");
        }
        return provinciaService.buscarProvincia(idProvincia);
    }

    public Departamento buscarDepartamento(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El departamento no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El departamento no existe o fue eliminado."));
    }

    public Departamento buscarDepartamentoPorNombre(String nombre) throws ErrorServiceException {
        validarNombre(nombre);
        return repository.findByEliminadoFalseOrderByNombreAsc().stream()
                .filter(d -> TextoUtils.mismoNombre(d.getNombre(), nombre))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("El departamento no existe o fue eliminado."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarDepartamento(String id, String nombre, String idProvincia) throws ErrorServiceException {
        Departamento departamento = buscarDepartamento(id);
        Provincia provincia = buscarProvinciaPadre(idProvincia);
        validar(nombre, provincia, id);
        departamento.setNombre(nombre.strip());
        departamento.setProvincia(provincia);
        repository.save(departamento);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarDepartamento(String id) throws ErrorServiceException {
        Departamento departamento = buscarDepartamento(id);
        if (localidadRepository.existsByDepartamento_IdAndEliminadoFalse(id)) {
            throw new ErrorServiceException(
                    "No se puede eliminar el departamento porque tiene localidades activas.");
        }
        departamento.setEliminado(true);
        repository.save(departamento);
    }

    /** Departamentos de una provincia, incluidos los eliminados. Sin provincia, devuelve todos. */
    public List<Departamento> listarDepartamento(String idProvincia) {
        return sinFiltro(idProvincia) ? repository.findAllByOrderByNombreAsc()
                : repository.findByProvincia_IdOrderByNombreAsc(idProvincia);
    }

    /** Departamentos activos de una provincia. Sin provincia, devuelve todos los activos. */
    public List<Departamento> listarDepartamentoActivo(String idProvincia) {
        return sinFiltro(idProvincia) ? repository.findByEliminadoFalseOrderByNombreAsc()
                : repository.findByProvincia_IdAndEliminadoFalseOrderByNombreAsc(idProvincia);
    }

    /** Filas del listado del ABM: nombre y provincia. */
    public List<FilaTablaDTO> listarFilaDepartamentoActivo(String idProvincia) {
        return listarDepartamentoActivo(idProvincia).stream()
                .map(d -> new FilaTablaDTO(d.getId(), d.getNombre(),
                        List.of(d.getNombre(), d.getProvincia().getNombre())))
                .toList();
    }

    /** Opciones para el filtro: id → "Departamento (Provincia)", ordenadas por nombre. */
    public Map<String, String> listarOpcionDepartamentoActivo() {
        Map<String, String> opciones = new LinkedHashMap<>();
        listarDepartamentoActivo(null).forEach(d -> opciones.put(d.getId(),
                d.getNombre() + " (" + d.getProvincia().getNombre() + ")"));
        return opciones;
    }

    /**
     * Opciones agrupadas para elegir país → provincia → departamento en un solo select:
     * "País / Provincia" → (id → nombre del departamento).
     */
    public Map<String, Map<String, String>> listarOpcionDepartamentoActivoAgrupado() {
        Map<String, Map<String, String>> grupos = new LinkedHashMap<>();
        listarDepartamentoActivo(null).stream()
                .sorted(Comparator.comparing((Departamento d) -> d.getProvincia().getPais().getNombre())
                        .thenComparing(d -> d.getProvincia().getNombre())
                        .thenComparing(Departamento::getNombre))
                .forEach(d -> grupos
                        .computeIfAbsent(d.getProvincia().getPais().getNombre() + " / " + d.getProvincia().getNombre(),
                                k -> new LinkedHashMap<>())
                        .put(d.getId(), d.getNombre()));
        return grupos;
    }

    private boolean sinFiltro(String id) {
        return id == null || id.isBlank();
    }
}
