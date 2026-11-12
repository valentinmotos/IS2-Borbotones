package com.dulcemayor.security;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.util.WebUtils;

import com.dulcemayor.exception.ErrorServiceException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Identidad temporal, validada por Spring Security y ligada a la sesión del navegador. */
public final class GoogleOAuthFlow {

    public static final String BASE = "/api/auth/google";
    public static final String EMISOR = "https://accounts.google.com";
    public static final String CALLBACK = BASE + "/callback";
    public static final String INICIAR = BASE + "/iniciar";
    private static final String ATRIBUTO = GoogleOAuthFlow.class.getName();

    private GoogleOAuthFlow() {
    }

    public record Identidad(String flujo, String sujeto, String correo, Instant vence) {
    }

    public static boolean guardar(HttpServletRequest request, OidcUser usuario) {
        String correo = usuario.getEmail();
        String sujeto = usuario.getSubject();
        if (correo == null || correo.isBlank() || correo.length() > 254
                || !Boolean.TRUE.equals(usuario.getEmailVerified()) || sujeto == null
                || sujeto.isBlank() || sujeto.length() > 255) {
            return false;
        }
        HttpSession sesion = request.getSession();
        sesion.setMaxInactiveInterval(600);
        synchronized (WebUtils.getSessionMutex(sesion)) {
            sesion.setAttribute(ATRIBUTO, new Identidad(UUID.randomUUID().toString(), sujeto,
                    correo.strip().toLowerCase(Locale.ROOT), Instant.now().plusSeconds(300)));
        }
        return true;
    }

    public static Identidad consultar(HttpSession sesion) {
        if (sesion == null || !(sesion.getAttribute(ATRIBUTO) instanceof Identidad identidad)) {
            return null;
        }
        return identidad.vence().isAfter(Instant.now()) ? identidad : null;
    }

    public static <T> T consumir(HttpServletRequest request, String flujo,
            Function<Identidad, T> operacion) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            throw vencido();
        }
        synchronized (WebUtils.getSessionMutex(sesion)) {
            Identidad identidad = consultar(sesion);
            if (identidad == null || !identidad.flujo().equals(flujo)) {
                throw vencido();
            }
            T resultado = operacion.apply(identidad);
            sesion.removeAttribute(ATRIBUTO);
            return resultado;
        }
    }

    public static void limpiar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            synchronized (WebUtils.getSessionMutex(sesion)) {
                sesion.removeAttribute(ATRIBUTO);
            }
        }
    }

    private static ErrorServiceException vencido() {
        return new ErrorServiceException("El acceso con Google venció. Volvé a iniciarlo.");
    }
}
