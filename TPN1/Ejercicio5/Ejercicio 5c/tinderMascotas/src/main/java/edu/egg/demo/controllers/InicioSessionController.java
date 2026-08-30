/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.egg.demo.controllers;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class InicioSessionController {
    
    @GetMapping({"/iniciarSession"})
    public String iniciarSession(Model model) {
        try {
            return "view/iniciarSession";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
    @PostMapping({"/iniciarSession"})
    public String iniciarSessionPost(Model model, @RequestParam String email, @RequestParam String password, HttpSession session) {
        try {
            boolean correcto = true;
            // -------------------------------------
            // INGRESAR SERVICIOS DE INICIO DE SESSION AQUI, correcto si es valido
            // -------------------------------------
            // -------------------------------------
            if (correcto) {
                session.setAttribute("usuario", email);
                return "redirect:/mascotas";
            }
            return "view/iniciarSession";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
}
