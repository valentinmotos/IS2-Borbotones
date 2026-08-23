package com.example.demo.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// @Controller: identifica la clase como controlador MVC encargado de recibir solicitudes y devolver vistas.
@Controller
public class InicioController {

	// @GetMapping: asocia una solicitud HTTP GET con el metodo que atiende esa ruta.
	@GetMapping("/")
	public String inicio() {
		return "view/inicio";
	}
}
