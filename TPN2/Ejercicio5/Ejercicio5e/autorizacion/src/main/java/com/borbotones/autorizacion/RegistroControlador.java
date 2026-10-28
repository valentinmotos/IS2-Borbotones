package com.borbotones.autorizacion;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegistroControlador {
    private final JdbcUserDetailsManager usuarios;
    private final PasswordEncoder encoder;
    private final String cliente;
    public RegistroControlador(JdbcUserDetailsManager usuarios, PasswordEncoder encoder, @Value("${app.oauth.client-url}") String cliente) {
        this.usuarios=usuarios; this.encoder=encoder; this.cliente=cliente;
    }
    @GetMapping("/registro")
    String formulario(Model model) { model.addAttribute("cliente",cliente); return "registro"; }
    @PostMapping("/registro")
    String registrar(@RequestParam String username, @RequestParam String password, @RequestParam String confirmacion, Model model) {
        String nombre=username.trim().toLowerCase(Locale.ROOT);
        String error=null;
        if (!nombre.matches("[a-z0-9_]{3,40}")) error="Usa entre 3 y 40 letras minusculas, numeros o guion bajo para el usuario.";
        else if (password.length()<8 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            error="La clave debe tener entre 8 y 64 caracteres y hasta 72 bytes.";
        else if (!password.equals(confirmacion)) error="Las claves no coinciden.";
        else if (usuarios.userExists(nombre)) error="Ese usuario ya existe.";
        if (error==null) {
            try { usuarios.createUser(User.withUsername(nombre).password(encoder.encode(password)).roles("USUARIO").build()); }
            catch (DataIntegrityViolationException ex) { error="Ese usuario ya existe."; }
        }
        model.addAttribute("cliente",cliente);
        if (error!=null) { model.addAttribute("error",error); model.addAttribute("username",nombre); return "registro"; }
        model.addAttribute("exito",true); return "registro";
    }
}
