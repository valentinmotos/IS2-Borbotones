package com.dulcemayor.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * E1-05 · criterio de punta a punta (H2): se sube una imagen, se ve por su URL (pública y con
 * caché) y un archivo no permitido devuelve el error con su campo. Incluye las reglas de rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImagenIntegrationTest {

    private static final byte[] PNG = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4 };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static MockMultipartHttpServletRequestBuilder subir(String nombre, byte[] contenido) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/imagenes")
                .file(new MockMultipartFile("archivo", nombre, "image/png", contenido))
                .param("tipoImagen", "PRODUCTO");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void seSubeYSeVePorSuUrl() throws Exception {
        String cuerpo = mvc.perform(subir("alfajor.png", PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.mime").value("image/png"))
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(cuerpo).get("url").asText();

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=2592000")))
                .andExpect(content().bytes(PNG));
    }

    @Test
    @WithAnonymousUser
    void laImagenSeVeSinSesionPeroNoSeSubeSinSesion() throws Exception {
        // 404 (y no 401): el GET es público aunque la imagen no exista
        mvc.perform(get("/api/imagenes/no-existe")).andExpect(status().isNotFound());
        mvc.perform(subir("a.png", PNG)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PREVENTOR")
    void soloJefeYAdministrativoSubenImagenes() throws Exception {
        mvc.perform(subir("a.png", PNG)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "JEFE")
    void unArchivoNoPermitidoMuestraElError() throws Exception {
        mvc.perform(subir("foto.png", "%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El archivo debe ser JPG, PNG o WEBP."))
                .andExpect(jsonPath("$.campo").value("archivo"));
    }
}
