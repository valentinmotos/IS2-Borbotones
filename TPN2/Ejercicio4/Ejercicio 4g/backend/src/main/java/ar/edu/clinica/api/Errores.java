package ar.edu.clinica.api;

import ar.edu.clinica.service.NegocioException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;

@RestControllerAdvice
public class Errores {
    @ExceptionHandler(NegocioException.class)
    ResponseEntity<?> negocio(NegocioException e) { return ResponseEntity.status(e.estado).body(Map.of("mensaje",e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacion(MethodArgumentNotValidException e) {
        String mensaje=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).collect(java.util.stream.Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("mensaje",mensaje));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,org.springframework.web.multipart.MultipartException.class,java.io.IOException.class})
    ResponseEntity<?> formato(Exception e) { return ResponseEntity.badRequest().body(Map.of("mensaje","Solicitud o archivo inválido. Fotos PNG/JPEG de hasta 2 MB.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflicto(Exception e) { return ResponseEntity.status(409).body(Map.of("mensaje","Los datos duplican un registro o incumplen una relación")); }
}
