package com.zero.ecommerce.controllers.cliente;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.NacionalidadService;
import com.zero.ecommerce.services.UsuarioService;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;
    private final NacionalidadService nacionalidadService;
    private final LocalidadService localidadService;
    private final UsuarioService usuarioService;

    public ClienteController(ClienteService clienteService,
                             NacionalidadService nacionalidadService,
                             LocalidadService localidadService,
                             UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.nacionalidadService = nacionalidadService;
        this.localidadService = localidadService;
        this.usuarioService = usuarioService;
    }

    private String resolverUsuarioId(Principal principal) throws ErrorServiceException {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            Usuario usuario = usuarioService.buscarUsuarioPorNombre( principal.getName() );
            return usuario.getId();
        }
        return usuarioService.usuarioActual()
                .map(Usuario::getId)
                .orElseThrow(() -> new ErrorServiceException("No se encontró un usuario autenticado."));
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Principal principal) {
        try {
            String usuarioId = resolverUsuarioId( principal );
            Optional<Cliente> clienteOpt = clienteService.buscarClientePorUsuario(usuarioId);
            Cliente cliente = clienteOpt.orElseGet(() -> {
                Cliente nuevo = new Cliente();
                nuevo.setDireccion(new Direccion());
                nuevo.setTelefono(new ContactoTelefonico());
                return nuevo;
            });

            if (cliente.getDireccion() == null) {
                cliente.setDireccion( new Direccion() );
            }
            if (cliente.getTelefono() == null) {
                cliente.setTelefono( new ContactoTelefonico() );
            }

            model.addAttribute("cliente", cliente);
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            Cliente cliente = new Cliente();
            cliente.setDireccion(new Direccion());
            cliente.setTelefono(new ContactoTelefonico());
            model.addAttribute("cliente", cliente);
        }

        cargarCombos(model);
        return "cliente/perfil";
    }

    @PostMapping("/perfil")
    public String guardarPerfil(@ModelAttribute("cliente") Cliente cliente,
                                @RequestParam(value = "nacionalidadId", required = false) String nacionalidadId,
                                @RequestParam(value = "localidadId", required = false) String localidadId,
                                @RequestParam(value = "telefonoCelular", required = false) String telefonoCelular,
                                @RequestParam(value = "calle", required = false) String calle,
                                @RequestParam(value = "numeracion", required = false) String numeracion,
                                @RequestParam(value = "barrio", required = false) String barrio,
                                @RequestParam(value = "manzanaPiso", required = false) String manzanaPiso,
                                @RequestParam(value = "casaDepartamento", required = false) String casaDepartamento,
                                @RequestParam(value = "referencia", required = false) String referencia,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirect) {

        try {
            String usuarioId = resolverUsuarioId(principal);
            clienteService.guardarOActualizarPerfil(
                    usuarioId,
                    cliente,
                    nacionalidadId,
                    localidadId,
                    telefonoCelular,
                    calle,
                    numeracion,
                    barrio,
                    manzanaPiso,
                    casaDepartamento,
                    referencia
            );

            redirect.addFlashAttribute("exito", "Perfil guardado con éxito.");
            return "redirect:/cliente/perfil";
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            if (cliente.getDireccion() == null) {
                cliente.setDireccion(new Direccion());
            }
            if (cliente.getTelefono() == null) {
                cliente.setTelefono(new ContactoTelefonico());
            }
            model.addAttribute("cliente", cliente);
            cargarCombos(model);
            return "cliente/perfil";
        }
    }

    private void cargarCombos(Model model) {
        model.addAttribute("nacionalidades", nacionalidadService.listarNacionalidadActiva());
        model.addAttribute("localidades", localidadService.listarLocalidadActivo(null));
        model.addAttribute("tiposDocumento", TipoDocumento.values());
    }
}