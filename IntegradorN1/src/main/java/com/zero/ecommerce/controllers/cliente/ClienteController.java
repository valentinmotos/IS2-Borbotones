package com.zero.ecommerce.controllers.cliente;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.ClienteForm;
import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.UsuarioService;

/** Perfil del cliente logueado (RF03 y RF05). */
@Controller
@RequestMapping("/cliente/perfil")
public class ClienteController {

    private static final String BASE = "/cliente/perfil";
    private final ClienteService service;
    private final UsuarioService usuarioService;

    public ClienteController(ClienteService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String perfil(Model model) {
        Optional<Cliente> cliente = service.buscarClientePorUsuario(usuarioLogueado().getId());
        model.addAttribute("pageTitle", "Mi perfil");
        model.addAttribute("perfilCargado", cliente.isPresent());
        model.addAttribute("imagenId", cliente.map(Cliente::getImagen).map(Imagen::getId).orElse(null));
        model.addAttribute("sexos", service.listarSexo());
        model.addAttribute("tiposDocumento", service.listarTipoDocumento());
        model.addAttribute("nacionalidades", service.listarNacionalidad());
        // Después de un error, los datos vuelven como flash y tienen prioridad sobre los guardados.
        if (!model.containsAttribute("clienteForm")) {
            model.addAttribute("clienteForm", cliente.map(ClienteForm::desde).orElseGet(ClienteForm::new));
        }
        if (!model.containsAttribute("direccionForm")) {
            model.addAttribute("direccionForm", cliente.map(Cliente::getDireccion).map(DireccionForm::desde)
                    .orElseGet(DireccionForm::new));
        }
        return "cliente/perfil";
    }

    @PostMapping
    public String guardar(@ModelAttribute ClienteForm clienteForm, @ModelAttribute DireccionForm direccionForm,
            @RequestParam(required = false) MultipartFile foto, RedirectAttributes flash) {
        try {
            service.guardarPerfilCliente(usuarioLogueado().getId(), clienteForm.getNombre(),
                    clienteForm.getApellido(), service.convertirSexo(clienteForm.getSexo()),
                    service.convertirFechaNacimiento(clienteForm.getFechaNacimiento()),
                    service.convertirTipoDocumento(clienteForm.getTipoDocumento()), clienteForm.getNumeroDocumento(),
                    clienteForm.getTelefono(), direccionForm, clienteForm.getNacionalidadId(), foto);
            flash.addFlashAttribute("exito", "Tus datos se guardaron correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("clienteForm", clienteForm);
            flash.addFlashAttribute("direccionForm", direccionForm);
        }
        return "redirect:" + BASE;
    }

    // /cliente/** exige el rol CLIENTE, así que siempre hay un usuario logueado.
    private Usuario usuarioLogueado() {
        return usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
