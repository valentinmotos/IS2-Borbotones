package com.example.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReporteTests {
    @Autowired MockMvc mvc;
    @Autowired RestTemplate rest;
    MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        servidor = MockRestServiceServer.createServer(rest);
    }

    @Test
    void descargarPdfConservaContenidoYEncabezados() throws Exception {
        byte[] archivo = "%PDF-1.7 archivo del servidor".getBytes();
        servidor.expect(requestTo("http://localhost:8000/api/reportes/personas-alquileres.pdf"))
                .andRespond(withSuccess(archivo, MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"personas-alquileres.pdf\""));
        mvc.perform(get("/reportes/personas-alquileres.pdf")).andExpect(status().isOk())
                .andExpect(content().bytes(archivo)).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"personas-alquileres.pdf\""));
        servidor.verify();
    }

    @Test
    void descargarExcelConservaContenidoYEncabezados() throws Exception {
        byte[] archivo = new byte[]{80, 75, 3, 4, 0, 1, 2, 3};
        String tipo = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        servidor.expect(requestTo("http://localhost:8000/api/reportes/libros-disponibles.xlsx"))
                .andRespond(withSuccess(archivo, MediaType.parseMediaType(tipo))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libros-disponibles.xlsx\""));
        mvc.perform(get("/reportes/libros-disponibles.xlsx")).andExpect(status().isOk())
                .andExpect(content().bytes(archivo)).andExpect(content().contentType(tipo))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libros-disponibles.xlsx\""));
        servidor.verify();
    }

    @Test
    void pantallasMuestranBotonesDeDescarga() throws Exception {
        servidor.expect(requestTo("http://localhost:8000/api/personas"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://localhost:8000/api/libros"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mvc.perform(get("/personas")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/reportes/personas-alquileres.pdf")));
        mvc.perform(get("/libros")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/reportes/libros-disponibles.xlsx")));
        servidor.verify();
    }
}
