package com.example.mascotas.errores;

import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ErroresApi extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> negocio(ApiException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Tinder Mascotas\", charset=\"UTF-8\"");
        return new ResponseEntity<>(body, headers, ex.getStatus());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> duplicado(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El recurso ya existe o viola una restriccion de datos"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Los datos enviados no son validos");
        body.setProperty("errores", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList());
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
