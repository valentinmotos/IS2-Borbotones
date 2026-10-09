package com.example.mascotas.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Las rutas del frontend sirven la misma entrada; los datos se obtienen por REST. */
@Controller
public class InterfazControlador {
    @GetMapping({"/login", "/registro", "/inicio", "/usuario/editar-perfil",
            "/mascota/mis-mascotas", "/mascota/editar-perfil", "/mascota/explorar-mascotas",
            "/mascota/mascotas-de-baja", "/votos/recibidos", "/matches", "/logout"})
    public String interfaz() {
        return "forward:/index.html";
    }
}
