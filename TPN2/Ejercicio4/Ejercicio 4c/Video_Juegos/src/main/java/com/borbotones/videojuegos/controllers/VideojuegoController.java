package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.CategoriaService;
import com.borbotones.videojuegos.services.EstudioService;
import com.borbotones.videojuegos.services.VideojuegoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;

@Controller
public class VideojuegoController extends BaseControllerTemplate<Videojuego> {

    private final VideojuegoService videojuegoService;
    private final CategoriaService categoriaService;
    private final EstudioService estudioService;

    public VideojuegoController(VideojuegoService videojuegoService,
                                CategoriaService categoriaService,
                                EstudioService estudioService) {
        super(videojuegoService);
        this.videojuegoService = videojuegoService;
        this.categoriaService = categoriaService;
        this.estudioService = estudioService;
    }

    /* ---- Alta ---- */

    @GetMapping("/altaVideojuego")
    public String altaVideojuego(Model model) {
        try {
            prepararAlta(model);
            return "view/videojuego/editVideojuego";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/altaVideojuego")
    public String guardarVideojuego(
            @RequestParam("archivo") MultipartFile archivo,
            @Valid @ModelAttribute("videojuego") Videojuego videojuego,
            BindingResult result,
            Model model
    ) {
        try {
            prepararModeloFormulario(model, "alta");
            if (result.hasErrors()) {
                return "view/videojuego/editVideojuego";
            }
            if (archivo.isEmpty()) {
                model.addAttribute("errorImagenMsg", "La imagen es requerida");
                return "view/videojuego/editVideojuego";
            }
            if (!validarExtension(archivo)) {
                model.addAttribute("errorImagenMsg", "La extension no es valida");
                return "view/videojuego/editVideojuego";
            }
            if (archivo.getSize() >= 15000000) {
                model.addAttribute("errorImagenMsg", "El peso excede 15MB");
                return "view/videojuego/editVideojuego";
            }
            String ruta = "C://Videojuegos/imagenes";
            int index = archivo.getOriginalFilename().indexOf(".");
            String extension = "." + archivo.getOriginalFilename().substring(index + 1);
            String nombreFoto = Calendar.getInstance().getTimeInMillis() + extension;
            Path rutaAbsoluta = Paths.get(ruta + "//" + nombreFoto);
            if (rutaAbsoluta.getParent() != null) {
                Files.createDirectories(rutaAbsoluta.getParent());
            }
            Files.write(rutaAbsoluta, archivo.getBytes());
            videojuego.setImagen(nombreFoto);
            videojuegoService.saveOne(videojuego);
            return redireccionListado();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Modificar ---- */

    @GetMapping("/modificarVideojuego")
    public String modificarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            return prepararModificacion(id, model);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/videojuego/aceptarEditVideojuego")
    public String aceptarEditVideojuego(
            @RequestParam("archivo") MultipartFile archivo,
            @Valid @ModelAttribute("videojuego") Videojuego videojuego,
            BindingResult result,
            Model model
    ) {
        try {
            prepararModeloFormulario(model, "modificar");
            if (result.hasErrors()) {
                return "view/videojuego/editVideojuego";
            }
            if (!archivo.isEmpty()) {
                if (!validarExtension(archivo)) {
                    model.addAttribute("errorImagenMsg", "La extension no es valida");
                    return "view/videojuego/editVideojuego";
                }
                if (archivo.getSize() >= 15000000) {
                    model.addAttribute("errorImagenMsg", "El peso excede 15MB");
                    return "view/videojuego/editVideojuego";
                }
                String ruta = "C://Videojuegos/imagenes";
                Path rutaAbsoluta = Paths.get(ruta + "//" + videojuego.getImagen());
                if (rutaAbsoluta.getParent() != null) {
                    Files.createDirectories(rutaAbsoluta.getParent());
                }
                Files.write(rutaAbsoluta, archivo.getBytes());
            }
            videojuegoService.updateOne(videojuego, videojuego.getId());
            return redireccionListado();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Consultar ---- */

    @GetMapping("/consultarVideojuego")
    public String consultarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            return prepararConsulta(id, model);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Baja lógica ---- */

    @GetMapping("/bajaVideojuego")
    public String bajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            return prepararBaja(id, model);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/bajaVideojuego")
    public String confirmarBajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            return baja(id, model);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Helpers ---- */

    public boolean validarExtension(MultipartFile archivo) {
        try {
            ImageIO.read(archivo.getInputStream()).toString();
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    @Override protected Videojuego nuevaEntidad() { return new Videojuego(); }
    @Override protected String nombreAtributo() { return "videojuego"; }
    @Override protected String nombreColeccion() { return "videojuegos"; }
    @Override protected String vistaListado() { return "view/inicio"; }
    @Override protected String vistaFormulario() { return "view/videojuego/editVideojuego"; }
    @Override protected String redireccionListado() { return "redirect:/inicio"; }
    @Override protected String rutaListado() { return "/inicio"; }
    @Override protected String rutaAlta() { return "/altaVideojuego"; }
    @Override protected String rutaModificacionGet() { return "/modificarVideojuego"; }
    @Override protected String rutaModificacionPost() { return "/videojuego/aceptarEditVideojuego"; }
    @Override protected String rutaConsulta() { return "/consultarVideojuego"; }
    @Override protected String rutaBaja() { return "/bajaVideojuego"; }

    @Override
    protected void prepararModeloFormulario(Model model, String modo) throws Exception {
        super.prepararModeloFormulario(model, modo);
        model.addAttribute("categorias", categoriaService.findAll());
        model.addAttribute("estudios", estudioService.findAll());
    }

    private String prepararBaja(long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findById(id));
            return "view/videojuego/bajaVideojuego";
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    @Override
    protected String prepararFormulario(Model model, String modo) throws Exception {
        model.addAttribute("modo", modo);
        model.addAttribute("categorias", categoriaService.findAll());
        model.addAttribute("estudios", estudioService.findAll());
        return vistaFormulario();
    }
}
