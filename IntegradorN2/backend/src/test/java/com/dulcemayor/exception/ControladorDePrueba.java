package com.dulcemayor.exception;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Controller que existe solo en los tests: cada endpoint provoca un tipo de error
 * para verificar cómo lo responde GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/prueba-errores")
class ControladorDePrueba {

    record PruebaRequest(@NotBlank(message = "El nombre es obligatorio.") String nombre) {
    }

    @GetMapping("/negocio")
    void negocio() {
        throw new ErrorServiceException("El stock no alcanza para el pedido.", "cantidad");
    }

    @GetMapping("/duplicado")
    void duplicado() {
        throw new DuplicadoException("Ya existe una categoría con ese nombre.", "nombre");
    }

    @GetMapping("/no-encontrado")
    void noEncontrado() {
        throw new RecursoNoEncontradoException("No se encontró la categoría.");
    }

    @PostMapping("/validacion")
    void validacion(@Valid @RequestBody PruebaRequest request) {
    }

    @GetMapping("/archivo-grande")
    void archivoGrande() {
        throw new MaxUploadSizeExceededException(2 * 1024 * 1024);
    }

    @GetMapping("/inesperado")
    void inesperado() {
        throw new IllegalStateException("detalle interno que no debe llegar al cliente");
    }

    @GetMapping("/solo-jefe")
    @PreAuthorize("hasRole('JEFE')")
    String soloJefe() {
        return "ok";
    }
}
