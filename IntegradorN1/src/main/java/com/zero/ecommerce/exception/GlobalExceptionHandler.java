package com.zero.ecommerce.exception;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Atrapa las excepciones de negocio que no manejó el controller y vuelve a la página anterior
 * con el mensaje en el flash attribute "error".
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorServiceException.class)
    public String manejarErrorService(ErrorServiceException e, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:" + paginaAnterior(request);
    }

    /**
     * Devuelve la ruta del header Referer, o "/" si no viene. Se descarta el host para
     * no redirigir nunca fuera de la aplicación.
     */
    private String paginaAnterior(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            String ruta = uri.getRawPath();
            if (ruta == null || !ruta.startsWith("/")) {
                return "/";
            }
            return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
        } catch (URISyntaxException ex) {
            return "/";
        }
    }
}
