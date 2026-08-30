/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.egg.demo.controllers;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MascotaController {
    
    @GetMapping({"/mascotas"})
    public String verMascotas(Model model, HttpSession session) {
        try {
            String usuario = (String) session.getAttribute("usuario");
            if (usuario == null) {
                return "redirect:/iniciarSession";
            }
            
            // -------------------------------------
            // INGRESAR SERVICIOS LISTAR ANIMALES DISPONIBLES
            // AGREGAR A MODEL LAS MASCOTAS
            // -------------------------------------
            // -------------------------------------
            List<Map<String, String>> mascotas = List.of(
                    Map.of(
                            "id", "1",
                            "nombre", "Firulais",
                            "sexo", "M",
                            "like","false"
                    ),
                    Map.of(
                            "id", "2",
                            "nombre", "Luna",
                            "sexo", "H",
                            "like","true"
                    )
            );

            model.addAttribute("mascotas", mascotas);
            
            return "view/mascotas";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
}
