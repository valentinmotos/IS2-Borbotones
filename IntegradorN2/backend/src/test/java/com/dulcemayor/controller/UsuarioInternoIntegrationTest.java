package com.dulcemayor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * E2-05: el controller redefine los métodos del AbstractCrudController; estos tests cubren que las
 * rutas heredadas (POST, PUT, DELETE) siguen respondiendo, validan el body y aplican las reglas
 * sobre la propia cuenta con el usuario del token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioInternoIntegrationTest {

    private static final String CLAVE = "Prueba1234!";
    private static final String URL = "/api/usuarios/internos";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;

    @Test
    void elJefeCreaModificaYDaDeBajaUnPreventor() throws Exception {
        String jefe = login(nuevoUsuario(RolUsuario.JEFE));
        String correo = "preventor-" + UUID.randomUUID() + "@prueba.com";

        String alta = cuerpo(correo, RolUsuario.PREVENTOR);
        String cuerpo = enviar(post(URL), jefe, alta)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PREVENTOR"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(cuerpo).get("id").asText();

        enviar(put(URL + "/" + id), jefe, alta.replace("\"Pedro\"", "\"Pablo\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pablo"));
        enviar(put(URL + "/" + id), jefe, "{\"correo\":\"no-es-correo\",\"rol\":\"DISTRIBUIDOR\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("correo"));

        mvc.perform(delete(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isNoContent());
    }

    @Test
    void elJefeNoPuedeEditarseNiDarseDeBajaNiResetearseLaClave() throws Exception {
        Usuario usuario = nuevoUsuario(RolUsuario.JEFE);
        String jefe = login(usuario);
        String propio = URL + "/" + usuario.getId();

        enviar(put(propio), jefe, cuerpo(usuario.getNombreUsuario(), RolUsuario.ADMINISTRATIVO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No podés modificar tu propia cuenta desde esta pantalla."));
        mvc.perform(delete(propio).header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No podés dar de baja tu propia cuenta."));
        mvc.perform(post(propio + "/resetear-clave").header(HttpHeaders.AUTHORIZATION, jefe))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unAdministrativoNoAccedeAlAbm() throws Exception {
        String administrativo = login(nuevoUsuario(RolUsuario.ADMINISTRATIVO));

        enviar(post(URL), administrativo, cuerpo("x-" + UUID.randomUUID() + "@prueba.com", RolUsuario.PREVENTOR))
                .andExpect(status().isForbidden());
    }

    private ResultActions enviar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido, String token,
            String json) throws Exception {
        return mvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String cuerpo(String correo, RolUsuario rol) {
        return """
                {"correo":"%s","rol":"%s","nombre":"Pedro","apellido":"Prueba","fechaNacimiento":"1990-01-01",
                 "tipoDocumento":"DNI","numeroDocumento":"%d","legajo":"L-%s"}
                """.formatted(correo, rol, 10_000_000 + (long) (Math.random() * 80_000_000),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private Usuario nuevoUsuario(RolUsuario rol) {
        return usuarioService.crearUsuario(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com",
                rol, CLAVE, false);
    }

    private String login(Usuario usuario) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario() + "\",\"clave\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(cuerpo).get("token").asText();
    }
}
