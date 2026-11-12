package com.dulcemayor.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.LoginResponse;
import com.dulcemayor.dto.MensajeResponse;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.security.GoogleOAuthFlow;
import com.dulcemayor.security.GoogleOAuthFlow.Identidad;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.GoogleOAuthService;
import com.dulcemayor.service.GoogleOAuthService.Estado;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(GoogleOAuthFlow.BASE)
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
public class GoogleOAuthController {

    private static final String FLUJO = "X-Google-Flow";
    private final GoogleOAuthService google;

    public GoogleOAuthController(GoogleOAuthService google) {
        this.google = google;
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoResponse> estado(HttpServletRequest request, CsrfToken csrf,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Identidad identidad = GoogleOAuthFlow.consultar(request.getSession(false));
        String usuarioId = usuario != null && usuario.rol() == RolUsuario.MINORISTA ? usuario.id() : null;
        return sinCache(new EstadoResponse(google.estado(identidad), identidad == null ? null : identidad.flujo(),
                identidad == null ? null : identidad.correo(), google.tieneVinculo(usuarioId), csrf.getToken()));
    }

    @PostMapping("/sesion")
    public ResponseEntity<LoginResponse> iniciarSesion(HttpServletRequest request, @RequestHeader(FLUJO) String flujo) {
        return sinCache(GoogleOAuthFlow.consumir(request, flujo, google::iniciarSesion));
    }

    @PostMapping("/registro")
    public ResponseEntity<MensajeResponse> registrar(HttpServletRequest request, @RequestHeader(FLUJO) String flujo,
            @Valid @RequestBody RegistroMinoristaRequest datos) {
        String mensaje = GoogleOAuthFlow.consumir(request, flujo, identidad -> google.registrar(identidad, datos));
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new MensajeResponse(mensaje));
    }

    @PostMapping("/vincular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vincular(HttpServletRequest request, @RequestHeader(FLUJO) String flujo,
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody ClaveRequest datos) {
        GoogleOAuthFlow.consumir(request, flujo, identidad -> {
            google.vincular(usuario.id(), datos.clave(), identidad);
            return null;
        });
    }

    @PostMapping("/desvincular")
    public ResponseEntity<LoginResponse> desvincular(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClaveRequest datos) {
        return sinCache(google.desvincular(usuario.id(), datos.clave()));
    }

    private static <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }

    public record EstadoResponse(Estado estado, String flujo, String correo, boolean vinculada, String csrf) {
    }

    public record ClaveRequest(@NotBlank(message = "Ingresá tu clave actual.") @Size(max = 72) String clave) {
    }
}
