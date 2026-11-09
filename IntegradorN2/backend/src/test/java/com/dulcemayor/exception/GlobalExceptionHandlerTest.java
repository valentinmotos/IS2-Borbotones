package com.dulcemayor.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.config.SecurityConfig;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;

/**
 * Contrato "Errores" (E0-01): toda respuesta de error es JSON {mensaje, campo} con el código HTTP correcto.
 */
@WebMvcTest(controllers = ControladorDePrueba.class)
@Import({ SecurityConfig.class, ErroresSeguridadHandler.class, JwtService.class })
@ActiveProfiles("test")
@WithMockUser(roles = "ADMINISTRATIVO")
class GlobalExceptionHandlerTest {

    private static final String BASE = "/api/prueba-errores";

    @Autowired
    private MockMvc mvc;

    /** Lo usa JwtFiltro; en estos tests la sesión la simula @WithMockUser. */
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void errorDeNegocioDevuelve400ConMensajeYCampo() throws Exception {
        mvc.perform(get(BASE + "/negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").value("El stock no alcanza para el pedido."))
                .andExpect(jsonPath("$.campo").value("cantidad"));
    }

    @Test
    void duplicadoDevuelve409() throws Exception {
        mvc.perform(get(BASE + "/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una categoría con ese nombre."))
                .andExpect(jsonPath("$.campo").value("nombre"));
    }

    @Test
    void recursoNoEncontradoDevuelve404ConCampoNulo() throws Exception {
        mvc.perform(get(BASE + "/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró la categoría."))
                .andExpect(jsonPath("$.campo").isEmpty());
    }

    @Test
    void validacionDelDtoDevuelve400ConElCampoInvalido() throws Exception {
        mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre es obligatorio."))
                .andExpect(jsonPath("$.campo").value("nombre"));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no tiene un formato válido."));
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El recurso solicitado no existe."));
    }

    @Test
    void archivoGrandeDevuelve413() throws Exception {
        mvc.perform(get(BASE + "/archivo-grande"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.mensaje").value("El archivo supera el tamaño máximo permitido (2 MB)."));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        mvc.perform(get(BASE + "/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado. Intentá de nuevo más tarde."))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    @WithAnonymousUser
    void sinSesionEnEndpointProtegidoDevuelve401() throws Exception {
        mvc.perform(get(BASE + "/solo-jefe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Tenés que iniciar sesión para acceder a este recurso."));
    }

    @Test
    @WithMockUser(roles = "PREVENTOR")
    void rolSinPermisoDevuelve403() throws Exception {
        mvc.perform(get(BASE + "/solo-jefe"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tenés permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(roles = "JEFE")
    void rolConPermisoAccede() throws Exception {
        mvc.perform(get(BASE + "/solo-jefe"))
                .andExpect(status().isOk());
    }
}
