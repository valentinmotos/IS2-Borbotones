package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.CategoriaService;
import com.borbotones.videojuegos.services.EstudioService;
import com.borbotones.videojuegos.services.VideojuegoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;

@Controller
public class VideojuegoController {

    @Autowired
    private VideojuegoService videojuegoService;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private EstudioService estudioService;

    /* ---- Alta ---- */

    @GetMapping("/altaVideojuego")
    public String altaVideojuego(Model model) {
        try {
            model.addAttribute("videojuego", new Videojuego());
            model.addAttribute("categorias", categoriaService.findAll());
            model.addAttribute("estudios", estudioService.findAll());
            model.addAttribute("modo", "alta");
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
            model.addAttribute("categorias", categoriaService.findAll());
            model.addAttribute("estudios", estudioService.findAll());
            model.addAttribute("modo", "alta");
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
            return "redirect:/inicio";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Modificar ---- */

    @GetMapping("/modificarVideojuego")
    public String modificarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findById(id));
            model.addAttribute("categorias", categoriaService.findAll());
            model.addAttribute("estudios", estudioService.findAll());
            model.addAttribute("modo", "modificar");
            return "view/videojuego/editVideojuego";
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
            model.addAttribute("categorias", categoriaService.findAll());
            model.addAttribute("estudios", estudioService.findAll());
            model.addAttribute("modo", "modificar");
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
            return "redirect:/inicio";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Consultar ---- */

    @GetMapping("/consultarVideojuego")
    public String consultarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findByIdAndActivo(id));
            model.addAttribute("categorias", categoriaService.findAll());
            model.addAttribute("estudios", estudioService.findAll());
            model.addAttribute("modo", "consulta");
            return "view/videojuego/editVideojuego";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Baja lógica ---- */

    @GetMapping("/bajaVideojuego")
    public String bajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findById(id));
            return "view/videojuego/bajaVideojuego";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/bajaVideojuego")
    public String confirmarBajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            videojuegoService.deleteById(id);
            return "redirect:/inicio";
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
}
