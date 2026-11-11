package com.dulcemayor.service;

import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.UsuarioInternoRequest;
import com.dulcemayor.dto.UsuarioInternoResponse;
import com.dulcemayor.dto.mapper.UsuarioInternoMapper;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.Empleado;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Persona;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.repository.EmpleadoRepository;
import com.dulcemayor.repository.PersonaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.utils.CuitUtils;

/** ABM del personal interno, usando el Template Method de E0-08. */
@Service
@Transactional(readOnly = true)
public class UsuarioInternoService extends AbstractCrudService<Usuario, UsuarioInternoRequest, UsuarioInternoResponse> {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioService usuarioService;

    public UsuarioInternoService(UsuarioRepository usuarioRepository, UsuarioInternoMapper mapper,
            UsuarioService usuarioService, EmpleadoRepository empleadoRepository,
            PersonaRepository personaRepository) {
        super(usuarioRepository, mapper);
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.empleadoRepository = empleadoRepository;
        this.personaRepository = personaRepository;
    }

    @Transactional
    public UsuarioInternoResponse crearUsuarioInterno(UsuarioInternoRequest request) {
        UsuarioInternoResponse creado = crear(request);
        Usuario usuario = usuarioService.buscarUsuarioPorNombre(creado.correo());
        guardarPerfilEmpleado(usuario, request);
        return respuesta(usuario);
    }

    /** Modificación por el Jefe. {@code idSolicitante} es el usuario del token: no puede editarse a sí mismo. */
    @Transactional
    public UsuarioInternoResponse modificarUsuarioInterno(String id, UsuarioInternoRequest request,
            String idSolicitante) {
        if (esElMismo(id, idSolicitante)) {
            throw new ErrorServiceException("No podés modificar tu propia cuenta desde esta pantalla.");
        }
        modificar(id, request);
        Usuario usuario = usuarioService.buscarUsuario(id);
        guardarPerfilEmpleado(usuario, request);
        return respuesta(usuario);
    }

    @Transactional
    public void eliminarUsuarioInterno(String id, String idSolicitante) {
        if (esElMismo(id, idSolicitante)) {
            throw new ErrorServiceException("No podés dar de baja tu propia cuenta.");
        }
        eliminar(id);
    }

    @Transactional
    public UsuarioInternoResponse resetearClave(String id, String idSolicitante) {
        if (esElMismo(id, idSolicitante)) {
            throw new ErrorServiceException("No podés resetear tu propia clave desde esta pantalla.");
        }
        return respuesta(usuarioService.resetearClaveTemporal(id));
    }

    private static boolean esElMismo(String id, String idSolicitante) {
        return idSolicitante != null && idSolicitante.equals(id);
    }

    public List<UsuarioInternoResponse> listarUsuariosInternos() {
        return usuarioService.listarUsuariosInternos().stream().map(this::respuesta).toList();
    }

    @Override
    protected void validar(UsuarioInternoRequest request, String idActual) {
        if (request == null) throw new ErrorServiceException("Los datos del usuario son obligatorios.");
        if (idActual == null && request.rol() == RolUsuario.JEFE) {
            throw new ErrorServiceException("El ABM solo permite crear administrativos, preventores y distribuidores.", "rol");
        }
        usuarioService.validarUsuarioInterno(request.correo(), request.rol(), idActual);
        validarPerfilEmpleado(request, idActual);
        if (idActual != null) {
            Usuario actual = usuarioRepository.findByIdAndEliminadoFalse(idActual)
                    .orElseThrow(() -> new com.dulcemayor.exception.RecursoNoEncontradoException(
                            "El usuario no existe o fue dado de baja."));
            if (actual.isActivo() && actual.getRol() == RolUsuario.JEFE
                    && request.rol() != RolUsuario.JEFE && usuarioService.contarJefesActivos() <= 1) {
                throw new ErrorServiceException("No se puede cambiar el rol del último Jefe activo.", "rol");
            }
        }
    }

    @Override
    protected Usuario nuevaEntidad() {
        return new Usuario();
    }

    @Override
    protected void asignar(Usuario usuario, UsuarioInternoRequest request) {
        String correoAnterior = usuario.getNombreUsuario();
        RolUsuario rolAnterior = usuario.getRol();
        super.asignar(usuario, request);
        usuario.setNombreUsuario(UsuarioService.normalizarCorreo(request.correo()));
        if (usuario.getId() == null) {
            usuarioService.establecerClaveTemporal(usuario);
        } else if (!usuario.getNombreUsuario().equals(correoAnterior) || usuario.getRol() != rolAnterior) {
            usuario.setVersionToken(usuario.getVersionToken() + 1);
        }
    }

    @Override
    protected void antesDeEliminar(Usuario usuario) {
        if (usuario.isActivo() && usuario.getRol() == RolUsuario.JEFE
                && usuarioService.contarJefesActivos() <= 1) {
            throw new ErrorServiceException("No se puede dar de baja al último Jefe activo.");
        }
        usuario.setVersionToken(usuario.getVersionToken() + 1);
        empleadoRepository.findByUsuario_IdAndEliminadoFalse(usuario.getId()).ifPresent(empleado -> {
            empleado.setEliminado(true);
            empleadoRepository.save(empleado);
        });
    }

    private void validarPerfilEmpleado(UsuarioInternoRequest request, String idActual) {
        if (!requierePerfil(request.rol())) return;
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new ErrorServiceException("El nombre es obligatorio para un empleado.", "nombre");
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new ErrorServiceException("El apellido es obligatorio para un empleado.", "apellido");
        }
        if (request.fechaNacimiento() == null || !request.fechaNacimiento().isBefore(LocalDate.now())) {
            throw new ErrorServiceException("Ingresá una fecha de nacimiento válida.", "fechaNacimiento");
        }
        if (request.tipoDocumento() == null) {
            throw new ErrorServiceException("El tipo de documento es obligatorio.", "tipoDocumento");
        }
        if (request.numeroDocumento() == null || request.numeroDocumento().isBlank()) {
            throw new ErrorServiceException("El número de documento es obligatorio.", "numeroDocumento");
        }
        if (request.legajo() == null || request.legajo().isBlank()) {
            throw new ErrorServiceException("El legajo es obligatorio para un empleado.", "legajo");
        }
        String documento = normalizarDocumento(request.tipoDocumento(), request.numeroDocumento());
        validarFormatoDocumento(request.tipoDocumento(), documento);
        String legajo = request.legajo().strip();

        Empleado actual = idActual == null ? null
                : empleadoRepository.findByUsuario_IdAndEliminadoFalse(idActual).orElse(null);
        if (actual != null && !actual.getClass().equals(claseEmpleado(request.rol()))) {
            throw new ErrorServiceException(
                    "No se puede cambiar entre Preventor y Distribuidor porque cada rol tiene un perfil de empleado distinto.",
                    "rol");
        }
        if (empleadoRepository.existsByLegajo(legajo)
                && (actual == null || !legajo.equals(actual.getLegajo()))) {
            throw new DuplicadoException("Ya existe un empleado con ese legajo.", "legajo");
        }
        Persona existente = personaRepository.findByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), documento)
                .orElse(null);
        if (existente != null && (actual == null || !existente.getId().equals(actual.getId()))) {
            throw new DuplicadoException("Ya existe una persona registrada con ese documento.", "numeroDocumento");
        }
    }

    private void guardarPerfilEmpleado(Usuario usuario, UsuarioInternoRequest request) {
        if (!requierePerfil(usuario.getRol())) return;
        Empleado empleado = empleadoRepository.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElse(null);
        if (empleado == null) {
            empleado = usuario.getRol() == RolUsuario.PREVENTOR ? new Preventor() : new Distribuidor();
            empleado.setUsuario(usuario);
        }
        empleado.setNombre(request.nombre().strip());
        empleado.setApellido(request.apellido().strip());
        empleado.setFechaNacimiento(request.fechaNacimiento());
        empleado.setTipoDocumento(request.tipoDocumento());
        empleado.setNumeroDocumento(normalizarDocumento(request.tipoDocumento(), request.numeroDocumento()));
        empleado.setLegajo(request.legajo().strip());
        empleadoRepository.save(empleado);
    }

    private UsuarioInternoResponse respuesta(Usuario usuario) {
        UsuarioInternoResponse base = mapper.aRespuesta(usuario);
        Empleado empleado = empleadoRepository.findByUsuario_IdAndEliminadoFalse(usuario.getId()).orElse(null);
        return empleado == null ? base : new UsuarioInternoResponse(base.id(), base.correo(), base.rol(),
                base.claveTemporal(), base.activo(), empleado.getNombre(), empleado.getApellido(),
                empleado.getFechaNacimiento(), empleado.getTipoDocumento(), empleado.getNumeroDocumento(),
                empleado.getLegajo());
    }

    private boolean requierePerfil(RolUsuario rol) {
        return rol == RolUsuario.PREVENTOR || rol == RolUsuario.DISTRIBUIDOR;
    }

    private Class<? extends Empleado> claseEmpleado(RolUsuario rol) {
        return rol == RolUsuario.PREVENTOR ? Preventor.class : Distribuidor.class;
    }

    private String normalizarDocumento(TipoDocumento tipo, String numero) {
        if (tipo == TipoDocumento.CUIT) return CuitUtils.normalizar(numero);
        return numero.strip().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private void validarFormatoDocumento(TipoDocumento tipo, String documento) {
        if (tipo == TipoDocumento.DNI && !documento.matches("\\d{7,8}")) {
            throw new ErrorServiceException("El DNI debe tener 7 u 8 dígitos.", "numeroDocumento");
        }
        if (tipo == TipoDocumento.CUIT && !CuitUtils.esValido(documento)) {
            throw new ErrorServiceException("Ingresá un CUIT válido.", "numeroDocumento");
        }
        if (tipo == TipoDocumento.PASAPORTE && !documento.matches("[A-Z0-9-]{3,30}")) {
            throw new ErrorServiceException("El pasaporte debe tener entre 3 y 30 letras o números.", "numeroDocumento");
        }
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "El usuario no existe o fue dado de baja.";
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombreUsuario");
    }
}
