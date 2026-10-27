package com.example.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTests {
    @Autowired MockMvc mvc;
    @Autowired RestTemplate rest;
    MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        servidor = MockRestServiceServer.createServer(rest);
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/personas")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginGuardaTokenYLoEnviaAlServidor() throws Exception {
        servidor.expect(requestTo("http://localhost:8000/api/auth/login")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"abc123\",\"username\":\"admin\",\"rol\":\"ADMIN\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://localhost:8000/api/personas"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc123"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/login").session(sesion).param("username", "admin").param("password", "admin123"))
                .andExpect(redirectedUrl("/personas"));
        assertThat(sesion.getAttribute("token")).isEqualTo("abc123");
        mvc.perform(get("/personas").session(sesion)).andExpect(status().isOk());
        servidor.verify();
    }

    @Test
    void credencialesIncorrectasMuestranError() throws Exception {
        servidor.expect(requestTo("http://localhost:8000/api/auth/login")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        mvc.perform(post("/login").param("username", "admin").param("password", "mal"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }

    @Test
    void tokenVencidoCierraLaSesion() throws Exception {
        servidor.expect(requestTo("http://localhost:8000/api/personas")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        mvc.perform(get("/personas").sessionAttr("token", "vencido")).andExpect(redirectedUrl("/login?vencida"));
    }

    @Test
    void usuarioSinPermisosVeAccesoDenegado() throws Exception {
        servidor.expect(requestTo("http://localhost:8000/api/personas/1")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        mvc.perform(get("/personas/eliminar/1").sessionAttr("token", "usuario"))
                .andExpect(status().isOk()).andExpect(view().name("acceso-denegado"));
    }
}
