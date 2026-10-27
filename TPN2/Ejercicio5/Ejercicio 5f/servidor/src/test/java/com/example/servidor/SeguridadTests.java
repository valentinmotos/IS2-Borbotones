package com.example.servidor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTests {
    static final Path TEMP = crearDirectorio();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    static Path crearDirectorio() {
        try { return Files.createTempDirectory("seguridad-biblioteca-"); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMP.resolve("test.sqlite"));
    }

    String login(String username, String password) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode cuerpo = json.readTree(respuesta);
        return cuerpo.get("token").asText();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/personas")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/personas").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioPuedeConsultarPeroNoModificar() throws Exception {
        String token = login("usuario", "usuario123");
        mvc.perform(get("/api/libros").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(delete("/api/libros/1").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeModificar() throws Exception {
        String token = login("admin", "admin123");
        mvc.perform(post("/api/libros").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Ficciones\",\"paginas\":200}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Ficciones"));
    }
}
