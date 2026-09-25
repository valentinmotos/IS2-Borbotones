package com.zero.ecommerce.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.DepartamentoRepository;
import com.zero.ecommerce.repositories.ProvinciaRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class ProvinciaService {

    private final ProvinciaRepository repository;
    private final DepartamentoRepository departamentoRepository;
    private final PaisService paisService;

    public ProvinciaService(ProvinciaRepository repository, DepartamentoRepository departamentoRepository,
            PaisService paisService) {
        this.repository = repository;
        this.departamentoRepository = departamentoRepository;
        this.paisService = paisService;
    }

    // Devuelve la provincia creada (el diagrama dice void) porque el seeder necesita su id.
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Provincia crearProvincia(String nombre, String idPais) throws ErrorServiceException {
        validar(nombre, idPais);
        Provincia provincia = new Provincia();
        provincia.setNombre(nombre.strip());
        provincia.setPais(paisService.buscarPais(idPais));
        return repository.save(provincia);
    }

    public void validar(String nombre, String idPais) throws ErrorServiceException {
        validar(nombre, idPais, null);
    }

    private void validar(String nombre, String idPais, String idActual) throws ErrorServiceException {
        validarNombre(nombre);
        Pais pais = buscarPaisPadre(idPais);
        Optional<Provincia> duplicada = repository.findByPais_IdAndEliminadoFalseOrderByNombreAsc(pais.getId())
                .stream()
                .filter(p -> TextoUtils.mismoNombre(p.getNombre(), nombre))
                .findFirst();
        if (duplicada.isPresent() && !duplicada.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una provincia con ese nombre en el país seleccionado.");
        }
    }

    private void validarNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la provincia es obligatorio.");
        }
    }

    private Pais buscarPaisPadre(String idPais) throws ErrorServiceException {
        if (idPais == null || idPais.isBlank()) {
            throw new ErrorServiceException("El país es obligatorio.");
        }
        return paisService.buscarPais(idPais);
    }

    public Provincia buscarProvincia(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La provincia no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La provincia no existe o fue eliminada."));
    }

    public Provincia buscarProvinciaPorNombre(String nombre) throws ErrorServiceException {
        validarNombre(nombre);
        return repository.findByEliminadoFalseOrderByNombreAsc().stream()
                .filter(p -> TextoUtils.mismoNombre(p.getNombre(), nombre))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("La provincia no existe o fue eliminada."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarProvincia(String id, String nombre, String idPais) throws ErrorServiceException {
        Provincia provincia = buscarProvincia(id);
        validar(nombre, idPais, id);
        provincia.setNombre(nombre.strip());
        provincia.setPais(paisService.buscarPais(idPais));
        repository.save(provincia);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarProvincia(String id) throws ErrorServiceException {
        Provincia provincia = buscarProvincia(id);
        if (departamentoRepository.existsByProvincia_IdAndEliminadoFalse(id)) {
            throw new ErrorServiceException(
                    "No se puede eliminar la provincia porque tiene departamentos activos.");
        }
        provincia.setEliminado(true);
        repository.save(provincia);
    }

    /** Provincias de un país, incluidas las eliminadas. Sin país, devuelve todas. */
    public List<Provincia> listarProvincia(String idPais) {
        return sinFiltro(idPais) ? repository.findAllByOrderByNombreAsc()
                : repository.findByPais_IdOrderByNombreAsc(idPais);
    }

    /** Provincias activas de un país. Sin país, devuelve todas las activas. */
    public List<Provincia> listarProvinciaActivo(String idPais) {
        return sinFiltro(idPais) ? repository.findByEliminadoFalseOrderByNombreAsc()
                : repository.findByPais_IdAndEliminadoFalseOrderByNombreAsc(idPais);
    }

    /** Filas del listado del ABM: nombre y país. */
    public List<FilaTablaDTO> listarFilaProvinciaActivo(String idPais) {
        return listarProvinciaActivo(idPais).stream()
                .map(p -> new FilaTablaDTO(p.getId(), p.getNombre(),
                        List.of(p.getNombre(), p.getPais().getNombre())))
                .toList();
    }

    /** Opciones para los selects: id → "Provincia (País)", ordenadas por nombre. */
    public Map<String, String> listarOpcionProvinciaActivo() {
        Map<String, String> opciones = new LinkedHashMap<>();
        listarProvinciaActivo(null)
                .forEach(p -> opciones.put(p.getId(), p.getNombre() + " (" + p.getPais().getNombre() + ")"));
        return opciones;
    }

    private boolean sinFiltro(String id) {
        return id == null || id.isBlank();
    }
}
