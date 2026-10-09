package com.example.cliente.controller;

import com.example.cliente.dto.LibroDTO;
import com.example.cliente.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/libros")
public class LibroController {
    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("libros", libroService.listar());
        return "libros/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        cargarFormulario(model, libroService.nuevo());
        return "libros/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        cargarFormulario(model, libroService.buscar(id));
        return "libros/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute LibroDTO libro,
                          @RequestParam(value = "pdf", required = false) MultipartFile pdf, Model model) {
        try {
            libroService.guardar(libro, pdf);
        } catch (RestClientResponseException ex) {
            String mensaje = "No se pudo guardar el libro. Compruebe los datos y el archivo PDF.";
            try {
                ProblemDetail problema = ex.getResponseBodyAs(ProblemDetail.class);
                if (problema != null && problema.getDetail() != null) {
                    mensaje = problema.getDetail();
                }
            } catch (RuntimeException ignorada) {
                // El servidor puede devolver un error sin cuerpo JSON.
            }
            cargarFormulario(model, libro);
            model.addAttribute("error", mensaje);
            return "libros/formulario";
        } catch (IllegalArgumentException ex) {
            cargarFormulario(model, libro);
            model.addAttribute("error", ex.getMessage());
            return "libros/formulario";
        }
        return "redirect:/libros";
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> abrirPdf(@PathVariable Long id) {
        try {
            ResponseEntity<byte[]> pdf = libroService.abrirPdf(id);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            pdf.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .header("X-Content-Type-Options", "nosniff")
                    .body(pdf.getBody());
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "No se pudo abrir el PDF", ex);
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return "redirect:/libros";
    }

    private void cargarFormulario(Model model, LibroDTO libro) {
        model.addAttribute("libro", libro);
        model.addAttribute("personas", libroService.listarPersonas());
    }
}
