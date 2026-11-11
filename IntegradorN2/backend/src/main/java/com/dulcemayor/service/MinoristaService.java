package com.dulcemayor.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.dto.PerfilMinoristaRequest;
import com.dulcemayor.dto.PerfilMinoristaResponse;
import com.dulcemayor.dto.mapper.PerfilMinoristaMapper;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.MinoristaRepository;

/**
 * Minoristas. El registro y la activación son de E2-04 (RegistroMinoristaService).
 * E2-06: consultas para los puntos de venta. E2-07: perfil del minorista autenticado (datos
 * personales y dirección de entrega). El minorista sale siempre del token.
 */
@Service
@Transactional(readOnly = true)
public class MinoristaService {

    private static final String MINORISTA_NO_ENCONTRADO = "No se encontró el minorista.";
    /** Igual que IntegradorN1 (ClienteService.EDAD_MINIMA). */
    private static final int EDAD_MINIMA = 18;
    private static final int LARGO_MAXIMO_NOMBRE = 100;

    private final MinoristaRepository minoristaRepository;
    private final DireccionService direccionService;
    private final PerfilMinoristaMapper perfilMapper;

    public MinoristaService(MinoristaRepository minoristaRepository, DireccionService direccionService,
            PerfilMinoristaMapper perfilMapper) {
        this.minoristaRepository = minoristaRepository;
        this.direccionService = direccionService;
        this.perfilMapper = perfilMapper;
    }

    /** Minoristas activos ordenados por apellido y nombre. */
    public List<MinoristaOpcion> listarMinoristaActivo() {
        return minoristaRepository.findByEliminadoFalseOrderByApellidoAscNombreAsc().stream()
                .map(m -> new MinoristaOpcion(m.getId(), m.getNombre(), m.getApellido(), m.getNumeroDocumento()))
                .toList();
    }

    /** Para otros services: el minorista activo o 404. */
    public Minorista buscarEntidad(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException(MINORISTA_NO_ENCONTRADO);
        }
        return minoristaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(MINORISTA_NO_ENCONTRADO));
    }

    /** E2-07: perfil del minorista del token. */
    public PerfilMinoristaResponse buscarPerfil(String idUsuario) {
        return perfilMapper.aRespuesta(buscarPorUsuario(idUsuario));
    }

    /**
     * E2-07 (modificarMinorista del UML): datos personales y dirección de entrega. La primera vez
     * crea la dirección; después modifica la misma, así el minorista nunca queda con dos.
     */
    @Transactional
    public PerfilMinoristaResponse modificarMinorista(String idUsuario, PerfilMinoristaRequest request) {
        validar(request);
        Minorista minorista = buscarPorUsuario(idUsuario);
        minorista.setNombre(request.nombre().strip());
        minorista.setApellido(request.apellido().strip());
        minorista.setFechaNacimiento(request.fechaNacimiento());

        Direccion actual = minorista.getDireccionEntrega();
        Direccion direccion = actual == null
                ? direccionService.crearDireccion(request.calle(), request.numeracion(), request.barrio(),
                        request.manzanaPiso(), request.casaDepartamento(), request.referencia(),
                        request.latitud(), request.longitud(), request.localidadId())
                : direccionService.modificarDireccion(actual, request.calle(), request.numeracion(),
                        request.barrio(), request.manzanaPiso(), request.casaDepartamento(), request.referencia(),
                        request.latitud(), request.longitud(), request.localidadId());
        minorista.setDireccionEntrega(direccion);
        return perfilMapper.aRespuesta(minoristaRepository.save(minorista));
    }

    void validar(PerfilMinoristaRequest request) {
        if (request == null) {
            throw new ErrorServiceException("Los datos del perfil son obligatorios.");
        }
        validarTexto(request.nombre(), "El nombre", "nombre");
        validarTexto(request.apellido(), "El apellido", "apellido");
        LocalDate nacimiento = request.fechaNacimiento();
        if (nacimiento == null || !nacimiento.isBefore(LocalDate.now())) {
            throw new ErrorServiceException("Ingresá una fecha de nacimiento válida.", "fechaNacimiento");
        }
        if (nacimiento.plusYears(EDAD_MINIMA).isAfter(LocalDate.now())) {
            throw new ErrorServiceException("Tenés que ser mayor de edad.", "fechaNacimiento");
        }
    }

    private static void validarTexto(String valor, String nombre, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ErrorServiceException(nombre + " es obligatorio.", campo);
        }
        if (valor.strip().length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorServiceException(nombre + " no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.",
                    campo);
        }
    }

    private Minorista buscarPorUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new RecursoNoEncontradoException("No se encontró tu perfil de minorista.");
        }
        return minoristaRepository.findByUsuario_IdAndEliminadoFalse(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró tu perfil de minorista."));
    }
}
