package edu.egg.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VotoMascotaController {

    @PostMapping("/votarMascota")
    public String votarMascotaPost( Model model, @RequestParam String pet_id, HttpSession session) {

        try {

            String email = (String) session.getAttribute("usuario");
            
            System.out.println("Mascota: " + pet_id);
            System.out.println("Usuario: " + email);
            // -------------------------------------
            // INGRESAR SERVICIOS DE VOTAR MASCOTA AQUI
            //
            // pet_id = mascota a la que se dio Like
            // email  = usuario actual
            // -------------------------------------

            return "redirect:/mascotas";

        } catch (Exception e) {

            model.addAttribute("error", e.getMessage());

            return "error";
        }
    }
}