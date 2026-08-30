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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class RegistrarUsuarioController {
    
    @GetMapping({"/registroUsuario"})
    public String registroUsuario(Model model) {
        try {
            // -------------------------------------
            // LISTAR TODOS LOS SEXOS DISPONIBLES
            // ELIMINAR LA LISTA CREADA A MANO
            // -------------------------------------
            // -------------------------------------
            List<String> sexos = List.of("M", "H","Desconocido"); //ELIMINAR UNA VES COMPLETO
            model.addAttribute("sexos", sexos); //ELIMINAR UNA VES COMPLETO 
            return "view/registroUsuario";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
    @PostMapping({"/registroUsuario"})
    public String registroUsuarioPost(Model model, @RequestParam Map<String, String> parametros, HttpSession session) {
        try {
            
            String nombre = parametros.get("nombre");
            String apellido = parametros.get("apellido");
            String email = parametros.get("email");
            String password = parametros.get("password");
            String nombreMascota = parametros.get("nombreMascota");
            String sexo = parametros.get("sexo");
            
            boolean procesoCorrecto = true;
            // -------------------------------------
            // SERVICIO DE GUARDAR USUARIO Y MASCOTA
            // -------------------------------------
            // -------------------------------------
            if (procesoCorrecto){
                return "redirect:/iniciarSession";
            }
            return "view/registroUsuario";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
}
