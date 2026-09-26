package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.CompraProveedorForm;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.FacturaProveedorService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.ProveedorService;

@Controller
@RequestMapping("/admin/compras")
public class CompraProveedorController {

    private static final String BASE = "/admin/compras";
    private final FacturaProveedorService service;
    private final ProveedorService proveedorService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;

    public CompraProveedorController(FacturaProveedorService service, ProveedorService proveedorService,
            FormaDePagoService formaDePagoService, ProductoService productoService) {
        this.service = service;
        this.proveedorService = proveedorService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "compras";
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String proveedor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        List<FacturaProveedor> compras;
        try {
            compras = service.listarFacturaActivo(service.convertirEstado(estado), proveedor, desde, hasta);
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            compras = List.of();
        }
        model.addAttribute("pageTitle", "Compras a proveedor");
        model.addAttribute("compras", compras);
        model.addAttribute("totalCompras", compras.size());
        model.addAttribute("opcionesEstado", service.listarEstadoCompra());
        model.addAttribute("opcionesProveedor", opcionesProveedor());
        model.addAttribute("estado", estado);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "admin/compras/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("pageTitle", "Nueva compra a proveedor");
        if (!model.containsAttribute("compraForm")) {
            model.addAttribute("compraForm", CompraProveedorForm.nuevo());
        }
        model.addAttribute("opcionesProveedor", opcionesProveedor());
        model.addAttribute("opcionesFormaDePago", opcionesFormaDePago());
        model.addAttribute("opcionesProducto", opcionesProducto());
        return "admin/compras/formulario";
    }

    @PostMapping
    public String crear(@ModelAttribute("compraForm") CompraProveedorForm compraForm, BindingResult binding,
            RedirectAttributes flash) {
        // Una cantidad o un precio que no es un número deja un error de conversión en lugar de un 400.
        if (binding.hasErrors()) {
            flash.addFlashAttribute("error", "Revisá las cantidades y los precios de costo: tienen que ser números.");
            flash.addFlashAttribute("compraForm", compraForm);
            return "redirect:" + BASE + "/nueva";
        }
        try {
            FacturaProveedor compra = service.crearFactura(compraForm.getProveedorId(), compraForm.getFormaDePagoId(),
                    compraForm.getDetalles());
            flash.addFlashAttribute("exito", "Compra N.º " + compra.getNumeroFactura()
                    + " creada: quedó pedida. Podés avisarle al proveedor por WhatsApp.");
            return "redirect:" + BASE + "/" + compra.getId();
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("compraForm", compraForm);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        FacturaProveedor compra = buscarO404(id);
        model.addAttribute("pageTitle", "Compra N.º " + compra.getNumeroFactura());
        model.addAttribute("compra", compra);
        model.addAttribute("celular", compra.getProveedor().buscarCelularActivo().orElse(null));
        model.addAttribute("mensajeWhatsApp", service.armarMensajeWhatsApp(compra));
        return "admin/compras/detalle";
    }

    private Map<String, String> opcionesProveedor() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (Proveedor proveedor : proveedorService.listarProveedorActivo()) {
            opciones.put(proveedor.getId(), proveedor.getRazonSocial());
        }
        return opciones;
    }

    private Map<String, String> opcionesFormaDePago() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (FormaDePago formaDePago : formaDePagoService.listarFormaDePagoActivo()) {
            opciones.put(formaDePago.getId(),
                    formaDePago.getObservacion() + " (" + formaDePago.getTipoPago().getDescripcion() + ")");
        }
        return opciones;
    }

    // El texto incluye código, nombre y talle: es lo que filtra el buscador de cada renglón.
    private Map<String, String> opcionesProducto() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (Producto producto : productoService.listarProductoActivo()) {
            opciones.put(producto.getId(),
                    producto.getCodigo() + " · " + producto.getNombre() + " (talle " + producto.getTalle() + ")");
        }
        return opciones;
    }

    private FacturaProveedor buscarO404(String id) {
        try {
            return service.buscarFactura(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
