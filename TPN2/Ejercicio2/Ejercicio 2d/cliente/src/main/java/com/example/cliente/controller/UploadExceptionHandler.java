package com.example.cliente.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String demasiadoGrande(RedirectAttributes atributos) {
        atributos.addFlashAttribute("error", "El PDF no puede superar los 20 MB. Vuelva a completar el formulario.");
        return "redirect:/libros/nuevo";
    }
}
