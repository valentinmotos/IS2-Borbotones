package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.services.ClienteService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.url-base=https://tienda.zero.test" })
@AutoConfigureMockMvc
class NewsletterIntegrationTest {

    private final MockMvc mvc;
    private final ClienteService clienteService;
    private MockHttpSession sesionAdmin;

    NewsletterIntegrationTest(@Autowired MockMvc mvc, @Autowired ClienteService clienteService) {
        this.mvc = mvc;
        this.clienteService = clienteService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesionAdmin = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void muestraEstadoYVistaPreviaConEnlacesAbsolutos() throws Exception {
        mvc.perform(get("/admin/newsletter").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Newsletter de ofertas")))
                .andExpect(content().string(containsString("Enviar ahora")))
                .andExpect(content().string(containsString("/admin/newsletter/vista-previa")));

        mvc.perform(get("/admin/newsletter/vista-previa").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Ofertas que no te podés perder")))
                .andExpect(content().string(containsString("https://tienda.zero.test/producto/")))
                .andExpect(content().string(containsString("https://tienda.zero.test/imagen/")));
    }

    // El seeder carga clientes con perfil completo (E4-06): se dan de baja dentro de la transacción del test, que al
    // terminar se revierte, para probar el caso sin destinatarios.
    @Test
    @Transactional
    void envioManualExigeCsrfYMuestraSiNoHayDestinatarios() throws Exception {
        clienteService.listarClienteActivo().forEach(cliente -> cliente.setEliminado(true));

        mvc.perform(post("/admin/newsletter/enviar").session(sesionAdmin))
                .andExpect(status().isForbidden());

        MvcResult pagina = mvc.perform(get("/admin/newsletter").session(sesionAdmin)).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/admin/newsletter/enviar").session(sesionAdmin)
                        .param(token.getParameterName(), token.getToken()))
                .andExpect(redirectedUrl("/admin/newsletter"))
                .andExpect(flash().attribute("error",
                        "No hay clientes activos con perfil completo para recibirlo."));
    }

    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                        .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesion;
    }
}
