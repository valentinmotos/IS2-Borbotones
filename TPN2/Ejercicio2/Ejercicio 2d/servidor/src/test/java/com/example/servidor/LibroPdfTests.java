package com.example.servidor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LibroPdfTests {
    static final Path TEMP = crearDirectorio();
    static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    static Path crearDirectorio() {
        try {
            return Files.createTempDirectory("biblioteca-test-");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("app.biblioteca.directorio", () -> TEMP.resolve("pdfs").toString());
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMP.resolve("test.sqlite"));
    }

    MockMultipartFile libro(String titulo) {
        return new MockMultipartFile("libro", "", "application/json",
                ("{\"titulo\":\"" + titulo + "\",\"fecha\":2026,\"genero\":\"Novela\",\"paginas\":10,\"autor\":\"Autor\"}")
                        .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void crearConsultarEditarYEliminarPdf() throws Exception {
        var respuesta = mvc.perform(multipart("/api/libros").file(libro("Árbol / Azul"))
                .file(new MockMultipartFile("pdf", "original.pdf", "application/pdf", PDF)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pdfNombre").value("libro_arbol_azul_.pdf"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        Path archivo = TEMP.resolve("pdfs/libro_arbol_azul_.pdf");
        assertThat(Files.readAllBytes(archivo)).isEqualTo(PDF);
        mvc.perform(get("/api/libros/{id}/pdf", id)).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"libro_arbol_azul_.pdf\""))
                .andExpect(content().bytes(PDF));
        mvc.perform(multipart("/api/libros/{id}", id).file(libro("Titulo editado"))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pdfNombre").value("libro_arbol_azul_.pdf"));
        mvc.perform(get("/api/libros/{id}/pdf", id)).andExpect(content().bytes(PDF));
        mvc.perform(delete("/api/libros/{id}", id)).andExpect(status().isOk());
        assertThat(archivo).doesNotExist();
        mvc.perform(get("/api/libros/{id}/pdf", id)).andExpect(status().isNotFound());
    }

    @Test
    void rechazarArchivosInvalidosYSobrescritura() throws Exception {
        mvc.perform(multipart("/api/libros").file(libro("Invalido"))
                        .file(new MockMultipartFile("pdf", "falso.pdf", "application/pdf", "texto".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/libros").file(libro("Duplicado"))
                        .file(new MockMultipartFile("pdf", "libro.pdf", "application/pdf", PDF)))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/libros").file(libro("Duplicado"))
                        .file(new MockMultipartFile("pdf", "otro.pdf", "application/pdf", PDF)))
                .andExpect(status().isConflict());
        assertThat(Files.readAllBytes(TEMP.resolve("pdfs/libro_duplicado_.pdf"))).isEqualTo(PDF);
    }

    @Test
    void permitirLibroSinPdfEIgnorarRutasDelCliente() throws Exception {
        String respuesta = mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sin PDF\",\"pdfNombre\":\"../../privado.pdf\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pdfNombre").isEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        mvc.perform(get("/api/libros/{id}/pdf", id)).andExpect(status().isNotFound());
    }
}
