package com.example.cliente.controller;

import com.example.cliente.dto.LoginDTO;
import com.example.cliente.dto.TokenDTO;
import com.example.cliente.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Controller
public class LoginController {
    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String formulario(Model model) {
        model.addAttribute("login", new LoginDTO());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginDTO login, HttpServletRequest request, Model model) {
        try {
            TokenDTO token = authService.login(login);
            HttpSession sesion = request.getSession();
            request.changeSessionId();
            sesion.setAttribute("token", token.getToken());
            sesion.setAttribute("usuario", token.getUsername());
            sesion.setAttribute("rol", token.getRol());
            return "redirect:/personas";
        } catch (HttpClientErrorException.Unauthorized ex) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
        } catch (ResourceAccessException ex) {
            model.addAttribute("error", "No se pudo conectar con el servidor.");
        }
        login.setPassword(null);
        model.addAttribute("login", login);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }
}
