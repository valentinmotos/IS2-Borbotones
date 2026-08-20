package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.ServicioCategoria;
import com.borbotones.videojuegos.services.ServicioEstudio;
import com.borbotones.videojuegos.services.ServicioVideojuego;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;

@Controller
public class controladorVideojuego {

    @Autowired
    private ServicioVideojuego svcVideojuego;
    @Autowired
    private ServicioCategoria svcCategoria;
    @Autowired
    private ServicioEstudio svcEstudio;

    /* ---- Alta ---- */

    @GetMapping("/altaVideojuego")
    public String altaVideojuego(Model model) {
        try {
            model.addAttribute("videojuego", new Videojuego());
            model.addAttribute("categorias", svcCategoria.findAll());
            model.addAttribute("estudios", svcEstudio.findAll());
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
            model.addAttribute("categorias", svcCategoria.findAll());
            model.addAttribute("estudios", svcEstudio.findAll());
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
            Files.write(rutaAbsoluta, archivo.getBytes());
            videojuego.setImagen(nombreFoto);
            svcVideojuego.saveOne(videojuego);
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
            model.addAttribute("videojuego", svcVideojuego.findById(id));
            model.addAttribute("categorias", svcCategoria.findAll());
            model.addAttribute("estudios", svcEstudio.findAll());
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
            model.addAttribute("categorias", svcCategoria.findAll());
            model.addAttribute("estudios", svcEstudio.findAll());
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
                Files.write(rutaAbsoluta, archivo.getBytes());
            }
            svcVideojuego.updateOne(videojuego, videojuego.getId());
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
            model.addAttribute("videojuego", svcVideojuego.findByIdAndActivo(id));
            model.addAttribute("categorias", svcCategoria.findAll());
            model.addAttribute("estudios", svcEstudio.findAll());
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
            model.addAttribute("videojuego", svcVideojuego.findById(id));
            return "view/videojuego/bajaVideojuego";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/bajaVideojuego")
    public String confirmarBajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            svcVideojuego.deleteById(id);
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
