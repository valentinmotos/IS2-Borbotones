package com.example.mascotas.controladores;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ErroresApi {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", ex.getReason()==null ? "Solicitud invalida" : ex.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField()+": "+e.getDefaultMessage()).findFirst().orElse("Datos invalidos");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> json() { return ResponseEntity.badRequest().body(Map.of("mensaje", "JSON o valores invalidos")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicado() { return ResponseEntity.status(409).body(Map.of("mensaje", "El registro ya existe o entra en conflicto con los datos")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> archivo() { return ResponseEntity.status(413).body(Map.of("mensaje", "La foto debe pesar como maximo 2 MB")); }
}
