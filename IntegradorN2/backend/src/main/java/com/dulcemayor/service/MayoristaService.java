package com.dulcemayor.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.MayoristaRequest;
import com.dulcemayor.dto.MayoristaResponse;
import com.dulcemayor.dto.mapper.MayoristaMapper;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.MayoristaRepository;
import com.dulcemayor.repository.SucursalRepository;
import com.dulcemayor.utils.CuitUtils;
import com.dulcemayor.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class MayoristaService extends AbstractCrudService<Mayorista, MayoristaRequest, MayoristaResponse> {

    private final MayoristaRepository mayoristaRepository;
    private final SucursalRepository sucursalRepository;

    public MayoristaService(MayoristaRepository repositorio, MayoristaMapper mapper,
            SucursalRepository sucursalRepository) {
        super(repositorio, mapper);
        this.mayoristaRepository = repositorio;
        this.sucursalRepository = sucursalRepository;
    }

    @Transactional public MayoristaResponse crearMayorista(MayoristaRequest request) { return crear(request); }
    @Transactional public MayoristaResponse modificarMayorista(String id, MayoristaRequest request) { return modificar(id, request); }
    @Transactional public void eliminarMayorista(String id) { eliminar(id); }
    public MayoristaResponse buscarMayorista(String id) { return buscar(id); }
    public List<MayoristaResponse> listarMayoristaActivo() { return listarActivos(); }

    @Override
    protected void validar(MayoristaRequest request, String idActual) {
        if (request == null || request.razonSocial() == null || request.razonSocial().isBlank()) {
            throw new ErrorServiceException("La razón social es obligatoria.", "razonSocial");
        }
        if (request.cuit() == null || !CuitUtils.esValido(request.cuit())) {
            throw new ErrorServiceException("Ingresá un CUIT válido.", "cuit");
        }
        List<Mayorista> activos = mayoristaRepository.findByEliminadoFalse();
        if (idActual == null && !activos.isEmpty()) {
            throw new DuplicadoException("Ya existe una empresa activa.", "razonSocial");
        }
        boolean duplicado = activos.stream()
                .filter(m -> !m.getId().equals(idActual))
                .anyMatch(m -> m.getCuit().equals(CuitUtils.normalizar(request.cuit()))
                        || TextoUtils.mismoNombre(m.getRazonSocial(), request.razonSocial()));
        if (duplicado) throw new DuplicadoException("Ya existe una empresa con esa razón social o CUIT.", "cuit");
    }

    @Override protected Mayorista nuevaEntidad() { return new Mayorista(); }

    @Override
    protected void asignar(Mayorista mayorista, MayoristaRequest request) {
        mayorista.setRazonSocial(request.razonSocial().strip());
        mayorista.setCuit(CuitUtils.normalizar(request.cuit()));
    }

    @Override
    protected void antesDeEliminar(Mayorista mayorista) {
        if (sucursalRepository.existsByMayorista_IdAndEliminadoFalse(mayorista.getId())) {
            throw new ErrorServiceException("No se puede dar de baja la empresa porque tiene sucursales activas.");
        }
    }

    @Override protected String mensajeNoEncontrado() { return "No se encontró la empresa."; }
    @Override protected Sort orden() { return Sort.by("razonSocial"); }
}
