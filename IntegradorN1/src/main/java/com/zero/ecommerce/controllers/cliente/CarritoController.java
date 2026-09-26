package com.zero.ecommerce.controllers.cliente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador para la gestión del carrito de compras del cliente (E3-07 / RF17).
 * Todas las rutas bajo /cliente/** requieren autenticación con rol CLIENTE.
 */
@Controller
@RequestMapping("/cliente/carrito")
public class CarritoController {

    private static final String RUTA_CARRITO = "/cliente/carrito";

    private final CarritoService carritoService;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public CarritoController(CarritoService carritoService, ClienteService clienteService,
            UsuarioService usuarioService) {
        this.carritoService = carritoService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la pantalla del carrito con los productos agregados, precios vigentes, subtotales y total.
     * Si algún producto cambió de stock o precio, se ajusta automáticamente y se avisa al cliente.
     */
    @GetMapping
    public String verCarrito(Model model) {
        try {
            Usuario usuario = usuarioLogueado();
            Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuario);

            // Revalidar stock y precios actuales al abrir el carrito (RF17)
            List<String> avisosStock = carritoService.sincronizarAjustes(cliente.getId());
            OrdenCompra carrito = carritoService.obtenerCarrito(cliente.getId());

            model.addAttribute("pageTitle", "Mi Carrito de Compras");
            model.addAttribute("carrito", carrito);
            model.addAttribute("detalles", carrito.getDetalles().stream().filter(d -> !d.isEliminado()).toList());
            model.addAttribute("perfilCompleto", clienteService.perfilCompleto(usuario.getId()));
            model.addAttribute("avisosStock", avisosStock);

            return "cliente/carrito";
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "cliente/carrito";
        }
    }

    /**
     * Contrato acordado en el kickoff: POST /cliente/carrito/agregar con idProducto y cantidad.
     */
    @PostMapping("/agregar")
    public String agregarProducto(@RequestParam String idProducto,
            @RequestParam(defaultValue = "1") int cantidad,
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request,
            RedirectAttributes flash) {
        try {
            Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuarioLogueado());
            carritoService.agregarProducto(cliente.getId(), idProducto, cantidad);
            flash.addFlashAttribute("exito", "Producto agregado al carrito con éxito.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        if (returnUrl != null && !returnUrl.isBlank() && !returnUrl.contains("/login")) {
            return "redirect:" + returnUrl;
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/login") && !referer.contains("/carrito")) {
            return "redirect:" + referer;
        }

        return "redirect:" + RUTA_CARRITO;
    }

    /**
     * Modifica la cantidad de un ítem en el carrito.
     */
    @PostMapping("/cantidad")
    public String modificarCantidad(@RequestParam String idDetalle,
            @RequestParam int cantidad,
            RedirectAttributes flash) {
        try {
            Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuarioLogueado());
            carritoService.modificarCantidad(cliente.getId(), idDetalle, cantidad);
            flash.addFlashAttribute("exito", "Cantidad actualizada.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA_CARRITO;
    }

    /**
     * Quita un producto del carrito.
     */
    @PostMapping("/quitar")
    public String quitarProducto(@RequestParam String idDetalle, RedirectAttributes flash) {
        try {
            Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuarioLogueado());
            carritoService.quitarProducto(cliente.getId(), idDetalle);
            flash.addFlashAttribute("exito", "Producto eliminado del carrito.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA_CARRITO;
    }

    /**
     * Vacía completamente el carrito.
     */
    @PostMapping("/vaciar")
    public String vaciarCarrito(RedirectAttributes flash) {
        try {
            Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuarioLogueado());
            carritoService.vaciarCarrito(cliente.getId());
            flash.addFlashAttribute("exito", "Se vació el carrito.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA_CARRITO;
    }

    private Usuario usuarioLogueado() {
        return usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
