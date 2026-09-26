package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ClienteRepository;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;
    private final DireccionService direccionService;
    private final NacionalidadService nacionalidadService;
    private final LocalidadService localidadService;
    private final ContactoService contactoService;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioService usuarioService,
                          DireccionService direccionService,
                          NacionalidadService nacionalidadService,
                          LocalidadService localidadService,
                          ContactoService contactoService) {
        this.clienteRepository = clienteRepository;
        this.usuarioService = usuarioService;
        this.direccionService = direccionService;
        this.nacionalidadService = nacionalidadService;
        this.localidadService = localidadService;
        this.contactoService = contactoService;
    }

    /**
     * Busca el Cliente asociado al Usuario activo ingresado.
     */
    public Optional<Cliente> buscarClientePorUsuario(String usuarioId) throws ErrorServiceException {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ErrorServiceException("El ID de usuario es obligatorio.");
        }
        return clienteRepository.findByUsuarioIdAndEliminadoFalse(usuarioId);
    }

    /**
     * Asocia el cliente recién creado al usuario del sistema.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Cliente asociarClienteUsuario(Cliente cliente, String usuarioId) throws ErrorServiceException {
        Usuario usuario = usuarioService.buscarUsuario(usuarioId);
        cliente.setUsuario(usuario);
        return clienteRepository.save(cliente);
    }

    /**
     * Procesa los datos de la pantalla /cliente/perfil.
     * Si es la primera vez, crea el cliente y lo asocia al usuario. Las siguientes veces lo modifica.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Cliente guardarOActualizarPerfil(String usuarioId,
                                            Cliente clienteForm,
                                            String idNacionalidad,
                                            String idLocalidad,
                                            String numeroTelefono,
                                            String calle,
                                            String numeracion,
                                            String barrio,
                                            String manzanaPiso,
                                            String casaDepartamento,
                                            String referencia) throws ErrorServiceException {

        // 1. Validar presencia de campos obligatorios
        validarDatos(clienteForm, numeroTelefono, idLocalidad, idNacionalidad);

        // 2. Validación: Mayoría de edad (>= 18 años)
        if (Period.between(clienteForm.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new ErrorServiceException("El cliente debe ser mayor de edad.");
        }

        // 3. Validación: Documento único
        Optional<Cliente> existenteDoc = clienteRepository.findByNumeroDocumentoAndEliminadoFalse(clienteForm.getNumeroDocumento().strip());
        if (existenteDoc.isPresent()) {
            Cliente c = existenteDoc.get();
            if (c.getUsuario() == null || !c.getUsuario().getId().equals(usuarioId)) {
                throw new ErrorServiceException("El número de documento ya pertenece a otro cliente.");
            }
        }

        // 4. Buscar cliente existente para actualizar o instanciar uno nuevo
        Optional<Cliente> optCliente = buscarClientePorUsuario(usuarioId);
        Cliente cliente = optCliente.orElseGet(Cliente::new);

        // Mapeo de datos heredados de Persona
        cliente.setNombre(clienteForm.getNombre().strip());
        cliente.setApellido(clienteForm.getApellido().strip());
        cliente.setFechaNacimiento(clienteForm.getFechaNacimiento());
        cliente.setTipoDocumento(clienteForm.getTipoDocumento());
        cliente.setNumeroDocumento(clienteForm.getNumeroDocumento().strip());

        // Asignar Nacionalidad a través de NacionalidadService
        Nacionalidad nacionalidad = nacionalidadService.buscarNacionalidad(idNacionalidad);
        cliente.setNacionalidad(nacionalidad);

        // Validar Localidad previamente mediante LocalidadService
        Localidad localidad = localidadService.buscarLocalidad(idLocalidad);

        // Crear/Actualizar Dirección mediante DireccionService
        Direccion direccion = cliente.getDireccion();
        if (direccion == null) {
            direccion = direccionService.crearDireccion(calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia, localidad.getId());
        } else {
            direccionService.modificarDireccion(direccion.getId(), calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia, localidad.getId());
            direccion = direccionService.buscarDireccion(direccion.getId());
        }
        cliente.setDireccion(direccion);

        // Crear/Actualizar ContactoTelefonico mediante ContactoService
        ContactoTelefonico contacto = cliente.getTelefono();
        if (contacto == null) {
            contacto = contactoService.crearContactoTelefonico(numeroTelefono, TipoTelefono.CELULAR, TipoContacto.PERSONAL, null);
        } else {
            contactoService.modificarContactoTelefonico(contacto.getId(), numeroTelefono, TipoTelefono.CELULAR, TipoContacto.PERSONAL, null);
            contacto = (ContactoTelefonico) contactoService.buscarContacto(contacto.getId());
        }
        cliente.setTelefono(contacto);

        // 5. Persistir (crear y asociar o guardar cambios)
        if (cliente.getId() == null) {
            return asociarClienteUsuario(cliente, usuarioId);
        } else {
            return clienteRepository.save(cliente);
        }
    }

    private void validarDatos(Cliente cliente, String numeroTelefono, String idLocalidad, String idNacionalidad) throws ErrorServiceException {
        if (cliente == null) {
            throw new ErrorServiceException("Los datos del cliente son obligatorios.");
        }
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new ErrorServiceException("El nombre es obligatorio.");
        }
        if (cliente.getApellido() == null || cliente.getApellido().isBlank()) {
            throw new ErrorServiceException("El apellido es obligatorio.");
        }
        if (cliente.getFechaNacimiento() == null) {
            throw new ErrorServiceException("La fecha de nacimiento es obligatoria.");
        }
        if (cliente.getTipoDocumento() == null) {
            throw new ErrorServiceException("El tipo de documento es obligatorio.");
        }
        if (cliente.getNumeroDocumento() == null || cliente.getNumeroDocumento().isBlank()) {
            throw new ErrorServiceException("El número de documento es obligatorio.");
        }
        if (idNacionalidad == null || idNacionalidad.isBlank()) {
            throw new ErrorServiceException("La nacionalidad es obligatoria.");
        }
        if (numeroTelefono == null || numeroTelefono.isBlank()) {
            throw new ErrorServiceException("El teléfono celular es obligatorio.");
        }
        if (idLocalidad == null || idLocalidad.isBlank()) {
            throw new ErrorServiceException("La localidad es obligatoria.");
        }
    }

    public Cliente buscarCliente(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El cliente no existe o fue eliminado.");
        }
        return clienteRepository.findById(id)
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new ErrorServiceException("El cliente no existe o fue eliminado."));
    }

    public List<Cliente> listarClientesActivos() {
        return clienteRepository.findAll().stream()
                .filter(c -> !c.isEliminado())
                .toList();
    }
}