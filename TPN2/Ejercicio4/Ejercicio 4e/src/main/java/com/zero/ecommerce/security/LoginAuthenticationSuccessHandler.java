package com.zero.ecommerce.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.UsuarioRepository;
import com.zero.ecommerce.services.CarritoService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handler de éxito de autenticación.
 * - Redirige a empleados a /admin.
 * - Para clientes, maneja el retorno con ítem pendiente (E3-08): si un visitante tocó "Agregar al carrito"
 *   y fue al login, agrega el producto automáticamente a su carrito, genera el mensaje de confirmación
 *   con el link "Ver carrito" y lo devuelve a la página del producto.
 * - Respeta SavedRequest para redirección a URLs protegidas.
 */
@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final CarritoService carritoService;
    private final UsuarioRepository usuarioRepository;

    public LoginAuthenticationSuccessHandler(CarritoService carritoService, UsuarioRepository usuarioRepository) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        boolean empleado = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_JEFE")
                        || authority.getAuthority().equals("ROLE_ADMINISTRATIVO"));

        if (empleado) {
            response.sendRedirect(request.getContextPath() + "/admin");
            return;
        }

        // Es CLIENTE
        HttpSession session = request.getSession(false);
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        String idProductoPendiente = null;
        int cantidadPendiente = 1;
        String urlRetorno = null;

        // 1. Revisar si había parámetros de retorno guardados en la sesión (vía /login?idProducto=...)
        if (session != null) {
            idProductoPendiente = (String) session.getAttribute("pendienteIdProducto");
            Integer cant = (Integer) session.getAttribute("pendienteCantidad");
            if (cant != null) {
                cantidadPendiente = cant;
            }
            urlRetorno = (String) session.getAttribute("pendienteRetorno");

            session.removeAttribute("pendienteIdProducto");
            session.removeAttribute("pendienteCantidad");
            session.removeAttribute("pendienteRetorno");
        }

        // 2. Revisar si la SavedRequest de Spring Security contenía un POST de agregar al carrito
        if (idProductoPendiente == null && savedRequest != null) {
            String[] idParam = savedRequest.getParameterValues("idProducto");
            if (idParam != null && idParam.length > 0 && !idParam[0].isBlank()) {
                idProductoPendiente = idParam[0];
                String[] cantParam = savedRequest.getParameterValues("cantidad");
                if (cantParam != null && cantParam.length > 0) {
                    try {
                        cantidadPendiente = Integer.parseInt(cantParam[0]);
                    } catch (NumberFormatException ignored) {}
                }
                List<String> refererHeaders = savedRequest.getHeaderValues("referer");
                if (refererHeaders != null && !refererHeaders.isEmpty()) {
                    urlRetorno = refererHeaders.get(0);
                }
            }
        }

        // Si había un producto pendiente de agregar, se agrega al carrito del cliente autenticado
        if (idProductoPendiente != null && !idProductoPendiente.isBlank()) {
            try {
                String nombreUsuario = authentication.getName();
                Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario).orElse(null);
                if (usuario != null) {
                    Cliente cliente = carritoService.obtenerOCrearClienteParaUsuario(usuario);
                    carritoService.agregarProducto(cliente.getId(), idProductoPendiente, cantidadPendiente);
                    if (session != null) {
                        session.setAttribute("mensajeExitoFlash",
                                "Producto agregado a tu carrito. <a href=\"/cliente/carrito\" class=\"alert-link font-weight-bold ml-1\">Ver carrito</a>");
                    }
                }
            } catch (ErrorServiceException e) {
                if (session != null) {
                    session.setAttribute("mensajeErrorFlash", e.getMessage());
                }
            }
        }

        // Determinar destino de redirección
        if (urlRetorno != null && !urlRetorno.isBlank() && !urlRetorno.contains("/login") && !urlRetorno.contains("/agregar")) {
            requestCache.removeRequest(request, response);
            response.sendRedirect(urlRetorno);
        } else if (savedRequest != null && !savedRequest.getRedirectUrl().contains("/agregar") && !savedRequest.getRedirectUrl().contains("/login")) {
            response.sendRedirect(savedRequest.getRedirectUrl());
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
