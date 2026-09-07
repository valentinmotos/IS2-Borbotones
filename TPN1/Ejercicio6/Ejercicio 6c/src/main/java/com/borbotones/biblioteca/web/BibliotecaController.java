package com.borbotones.biblioteca.web;

import com.borbotones.biblioteca.model.*;
import com.borbotones.biblioteca.repo.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
public class BibliotecaController {
    private final LibroRepository libros; private final AutorRepository autores; private final EditorialRepository editoriales;
    private final UsuarioRepository usuarios; private final PrestamoRepository prestamos;
    public BibliotecaController(LibroRepository l, AutorRepository a, EditorialRepository e, UsuarioRepository u, PrestamoRepository p) {
        libros=l; autores=a; editoriales=e; usuarios=u; prestamos=p;
    }
    @GetMapping({"/", "/libros"}) String inicio(Model m) { cargar(m); return "index"; }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/registro") String registro(Model m) { m.addAttribute("usuario", new Usuario()); return "registro"; }
    @PostMapping("/registro") String guardarRegistro(@Valid @ModelAttribute Usuario usuario, BindingResult result, Model m) {
        if (result.hasErrors() || usuarios.findByMail(usuario.getMail()).isPresent()) { m.addAttribute("error", "Datos invalidos o correo ya registrado"); return "registro"; }
        usuario.setClave(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(usuario.getClave())); usuario.setRol(Rol.USER); usuarios.save(usuario); return "redirect:/login?registered";
    }
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/libros") String nuevoLibro(@Valid @ModelAttribute Libro libro, BindingResult result, @RequestParam Long autorId, @RequestParam Long editorialId, Model m) {
        if (result.hasErrors()) { cargar(m); m.addAttribute("error", "Revisa los datos del libro"); return "index"; }
        libro.setAutor(autores.findById(autorId).orElseThrow()); libro.setEditorial(editoriales.findById(editorialId).orElseThrow()); libros.save(libro); return "redirect:/";
    }
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/autores") String nuevoAutor(@RequestParam String nombre) { Autor a=new Autor(); a.setNombre(nombre); autores.save(a); return "redirect:/"; }
    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/admin/editoriales") String nuevaEditorial(@RequestParam String nombre) { Editorial e=new Editorial(); e.setNombre(nombre); editoriales.save(e); return "redirect:/"; }
    @PostMapping("/prestamos") String prestar(@RequestParam Long libroId, Authentication auth, Model m) {
        Libro libro=libros.findById(libroId).orElseThrow(); Usuario usuario=usuarios.findByMail(auth.getName()).orElseThrow();
        if (libro.getDisponibles() < 1) { cargar(m); m.addAttribute("error", "No hay ejemplares disponibles"); return "index"; }
        libro.setEjemplaresPrestados(libro.getEjemplaresPrestados()+1); libros.save(libro); Prestamo p=new Prestamo(); p.setLibro(libro); p.setUsuario(usuario); prestamos.save(p); return "redirect:/";
    }
    @PostMapping("/prestamos/{id}/devolver") String devolver(@PathVariable Long id) { Prestamo p=prestamos.findById(id).orElseThrow(); if(p.isAlta()){p.setAlta(false);p.setFechaDevolucion(LocalDate.now()); Libro l=p.getLibro();l.setEjemplaresPrestados(Math.max(0,l.getEjemplaresPrestados()-1));libros.save(l);prestamos.save(p);} return "redirect:/"; }
    private void cargar(Model m) { m.addAttribute("libros", libros.findAll()); m.addAttribute("autores", autores.findAll()); m.addAttribute("editoriales", editoriales.findAll()); m.addAttribute("prestamos", prestamos.findByAltaTrueOrderByFechaPrestamoDesc()); m.addAttribute("libro", new Libro()); }
}