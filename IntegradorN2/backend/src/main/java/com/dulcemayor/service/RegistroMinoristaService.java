package com.dulcemayor.service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.ActivarCuentaRequest;
import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.entity.ContactoCorreoElectronico;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.ContactoCorreoElectronicoRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PersonaRepository;
import com.dulcemayor.utils.CuitUtils;

/** Registro público, creación del perfil minorista y activación con código enviado por correo. */
@Service
@Transactional(readOnly = true)
public class RegistroMinoristaService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String MENSAJE_REENVIO = "Si hay una cuenta pendiente con ese correo, enviamos un nuevo código.";

    private final UsuarioService usuarioService;
    private final MinoristaRepository minoristaRepository;
    private final PersonaRepository personaRepository;
    private final ContactoCorreoElectronicoRepository contactoRepository;
    private final EmailService emailService;

    public RegistroMinoristaService(UsuarioService usuarioService, MinoristaRepository minoristaRepository,
            PersonaRepository personaRepository, ContactoCorreoElectronicoRepository contactoRepository,
            EmailService emailService) {
        this.usuarioService = usuarioService;
        this.minoristaRepository = minoristaRepository;
        this.personaRepository = personaRepository;
        this.contactoRepository = contactoRepository;
        this.emailService = emailService;
    }

    @Transactional
    public String registrar(RegistroMinoristaRequest request) {
        registrarPerfil(request);
        return "Te enviamos un código de activación. Revisá tu correo para continuar.";
    }

    @Transactional
    public MinoristaOpcion registrarPerfil(RegistroMinoristaRequest request) {
        validarDocumento(request.tipoDocumento(), request.numeroDocumento());
        String correo = request.correo().strip().toLowerCase(Locale.ROOT);
        String documento = normalizarDocumento(request.tipoDocumento(), request.numeroDocumento());
        if (personaRepository.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), documento)) {
            throw new DuplicadoException("Ya existe una persona registrada con ese documento.", "numeroDocumento");
        }

        String codigo = generarCodigo();
        Usuario usuario = usuarioService.crearUsuarioPendienteMinorista(correo, request.clave(), codigo);

        Minorista minorista = new Minorista();
        minorista.setNombre(request.nombre().strip());
        minorista.setApellido(request.apellido().strip());
        minorista.setFechaNacimiento(request.fechaNacimiento());
        minorista.setTipoDocumento(request.tipoDocumento());
        minorista.setNumeroDocumento(documento);
        minorista.setUsuario(usuario);
        Minorista guardado = minoristaRepository.save(minorista);

        ContactoCorreoElectronico contacto = new ContactoCorreoElectronico();
        contacto.setEmail(correo);
        contacto.setMinorista(guardado);
        contactoRepository.save(contacto);

        enviarCodigo(correo, request.nombre(), codigo);
        return new MinoristaOpcion(guardado.getId(), guardado.getNombre(), guardado.getApellido(),
                guardado.getNumeroDocumento());
    }

    @Transactional
    public String activar(ActivarCuentaRequest request) {
        usuarioService.activarCuentaMinorista(request.correo(), request.codigo());
        return "Tu cuenta quedó activada. Ya podés iniciar sesión.";
    }

    @Transactional
    public String reenviar(String correo) {
        String codigo = generarCodigo();
        if (usuarioService.prepararReenvioCodigoMinorista(correo, codigo)) {
            String normalizado = correo.strip().toLowerCase(Locale.ROOT);
            String nombre = minoristaRepository.findByUsuario_NombreUsuarioAndEliminadoFalse(normalizado)
                    .map(Minorista::getNombre).orElse("");
            enviarCodigo(normalizado, nombre, codigo);
        }
        return MENSAJE_REENVIO;
    }

    private void enviarCodigo(String correo, String nombre, String codigo) {
        emailService.enviar(correo, "Activá tu cuenta de Dulce Mayor", "correo/activacion-minorista",
                Map.of("nombre", nombre == null ? "" : nombre, "codigo", codigo));
    }

    private String generarCodigo() {
        return String.format(Locale.ROOT, "%06d", ALEATORIO.nextInt(1_000_000));
    }

    private void validarDocumento(TipoDocumento tipo, String numero) {
        if (tipo == null) {
            throw new ErrorServiceException("El tipo de documento es obligatorio.", "tipoDocumento");
        }
        if (numero == null || numero.isBlank()) {
            throw new ErrorServiceException("El número de documento es obligatorio.", "numeroDocumento");
        }
        if (tipo == TipoDocumento.CUIT && !CuitUtils.esValido(numero)) {
            throw new ErrorServiceException("Ingresá un CUIT válido.", "numeroDocumento");
        }
        String documento = normalizarDocumento(tipo, numero);
        if (tipo == TipoDocumento.DNI && !documento.matches("\\d{7,8}")) {
            throw new ErrorServiceException("El DNI debe tener 7 u 8 dígitos.", "numeroDocumento");
        }
        if (tipo == TipoDocumento.PASAPORTE && !documento.matches("[A-Z0-9-]{3,30}")) {
            throw new ErrorServiceException("El pasaporte debe tener entre 3 y 30 letras o números.", "numeroDocumento");
        }
    }

    private String normalizarDocumento(TipoDocumento tipo, String numero) {
        if (tipo == TipoDocumento.CUIT) return CuitUtils.normalizar(numero);
        return numero.strip().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
