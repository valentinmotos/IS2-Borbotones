package com.example.mascotas.servicios;

import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Service;

@Service
public class CookieServicio {

    public static final String NOMBRE_COOKIE = "usuarioRecordado";
    public static final int DURACION_DOS_DIAS = 2 * 24 * 60 * 60;

    public Cookie crear(String idUsuario) {
        Cookie cookie = new Cookie(NOMBRE_COOKIE, idUsuario);
        cookie.setMaxAge(DURACION_DOS_DIAS);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    public Cookie eliminar() {
        Cookie cookie = new Cookie(NOMBRE_COOKIE, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
