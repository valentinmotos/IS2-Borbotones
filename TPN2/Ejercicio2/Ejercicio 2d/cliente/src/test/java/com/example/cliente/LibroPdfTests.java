package com.example.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LibroPdfTests {
    @Autowired MockMvc mvc;
    @Autowired RestTemplate rest;
    MockRestServiceServer servidor;
    final String api = "http://localhost:8000/api/libros";

    @BeforeEach
    void preparar() {
        servidor = MockRestServiceServer.createServer(rest);
    }

    @Test
    void enviarArchivoYDatosComoMultipartAlServidor() throws Exception {
        servidor.expect(requestTo(api)).andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header(
                        HttpHeaders.CONTENT_TYPE, containsString("multipart/form-data")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(containsString("name=\"pdf\"")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(containsString("%PDF-1.4")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(containsString("\"titulo\":\"Ejemplo\"")))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        mvc.perform(multipart("/libros/guardar")
                        .file(new MockMultipartFile("pdf", "ejemplo.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("titulo", "Ejemplo").param("fecha", "2026").param("paginas", "10")
                        .param("autor", "Autor").param("genero", "Novela"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/libros"));
        servidor.verify();
    }

    @Test
    void mostrarEnlaceEnPestanaNuevaYServirPdf() throws Exception {
        servidor.expect(requestTo(api)).andRespond(withSuccess(
                "[{\"id\":1,\"titulo\":\"Ejemplo\",\"pdfNombre\":\"libro_ejemplo_.pdf\"}]", MediaType.APPLICATION_JSON));
        mvc.perform(get("/libros")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/libros/1/pdf\" target=\"_blank\"")));
        servidor.verify();
        servidor.reset();
        byte[] pdf = "%PDF-1.4".getBytes();
        servidor.expect(requestTo(api + "/1/pdf")).andRespond(withSuccess(pdf, MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"libro_ejemplo_.pdf\""));
        mvc.perform(get("/libros/1/pdf")).andExpect(status().isOk()).andExpect(content().bytes(pdf))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"libro_ejemplo_.pdf\""));
        servidor.verify();
    }

    @Test
    void mostrarErrorDeValidacionEnFormulario() throws Exception {
        servidor.expect(requestTo(api)).andRespond(withBadRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body("{\"status\":400,\"detail\":\"El archivo debe ser un PDF valido\"}"));
        servidor.expect(requestTo("http://localhost:8000/api/personas"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mvc.perform(multipart("/libros/guardar")
                        .file(new MockMultipartFile("pdf", "falso.pdf", "application/pdf", "texto".getBytes()))
                        .param("titulo", "Invalido"))
                .andExpect(status().isOk()).andExpect(view().name("libros/formulario"))
                .andExpect(content().string(containsString("El archivo debe ser un PDF valido")));
        servidor.verify();
    }
}
