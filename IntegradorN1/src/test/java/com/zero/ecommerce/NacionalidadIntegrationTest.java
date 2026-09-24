package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.config.DataSeeder;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.repositories.NacionalidadRepository;
import com.zero.ecommerce.services.NacionalidadService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class NacionalidadIntegrationTest {

    private static final String BASE = "/admin/nacionalidades";
    private final MockMvc mvc;
    private final NacionalidadService service;
    private final NacionalidadRepository repository;
    private final DataSeeder seeder;

    NacionalidadIntegrationTest(@Autowired MockMvc mvc, @Autowired NacionalidadService service,
            @Autowired NacionalidadRepository repository, @Autowired DataSeeder seeder) {
        this.mvc = mvc;
        this.service = service;
        this.repository = repository;
        this.seeder = seeder;
    }

    @Test
    void recorreAltaEdicionYBajaLogicaConFormulariosReales() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("nombre", " Japonesa "))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Nacionalidad creada correctamente."));
        Nacionalidad creada = service.buscarNacionalidadPorNombre("JAPONESA");
        String id = creada.getId();
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(BASE + "/" + id + "/editar")))
                .andExpect(content().string(containsString("data-target=\"#eliminar-" + id + "\"")))
                .andExpect(content().string(containsString("action=\"" + BASE + "/" + id + "/eliminar\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        String editar = BASE + "/" + id + "/editar";
        mvc.perform(get(editar)).andExpect(content().string(containsString("value=\"Japonesa\"")));
        mvc.perform(postConCsrf(editar, editar).param("nombre", "Coreana"))
                .andExpect(redirectedUrl(BASE));
        assertThat(service.buscarNacionalidad(id).getNombre()).isEqualTo("Coreana");
        mvc.perform(postConCsrf(BASE + "/" + id + "/eliminar", BASE))
                .andExpect(redirectedUrl(BASE));
        assertThat(repository.findById(id).orElseThrow().isEliminado()).isTrue();
        assertThat(service.listarNacionalidadActiva()).extracting(Nacionalidad::getId).doesNotContain(id);
        assertThat(service.listarNacionalidad()).extracting(Nacionalidad::getId).contains(id);
        mvc.perform(get(editar)).andExpect(status().isNotFound());
        mvc.perform(postConCsrf(editar, BASE + "/nueva").param("nombre", "Otra"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicadoVuelveConFlashYConservaElNombre() throws Exception {
        long cantidad = repository.count();
        MvcResult resultado = mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("nombre", " ESPAÑA "))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "Ya existe una nacionalidad con ese nombre."))
                .andExpect(flash().attribute("nombre", " ESPAÑA ")).andReturn();
        mvc.perform(get(BASE + "/nueva").flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("Ya existe una nacionalidad con ese nombre.")))
                .andExpect(content().string(containsString("value=\" ESPAÑA \"")));
        assertThat(repository.count()).isEqualTo(cantidad);
    }

    @Test
    void validaNombreEnServidor() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("nombre", " "))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "El nombre de la nacionalidad es obligatorio."));
    }

    @Test
    void rechazaMutacionesSinCsrfYBajaPorGet() throws Exception {
        String id = service.buscarNacionalidadPorNombre("Argentina").getId();
        mvc.perform(post(BASE).param("nombre", "Otra")).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + id + "/editar").param("nombre", "Otra"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + id + "/eliminar")).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + id + "/eliminar")).andExpect(status().isMethodNotAllowed());
        assertThat(service.buscarNacionalidad(id).isEliminado()).isFalse();
        mvc.perform(get(BASE + "/inexistente/editar")).andExpect(status().isNotFound());
    }

    @Test
    void seederCargaNacionalidadesYNoLasDuplica() throws Exception {
        assertThat(service.listarNacionalidadActiva()).extracting(Nacionalidad::getNombre)
                .contains("Argentina", "Chile", "España", "Brasil");
        long cantidad = repository.count();
        seeder.run();
        assertThat(repository.count()).isEqualTo(cantidad);
    }

    @Test
    void listadoVacioMuestraMensaje() throws Exception {
        for (Nacionalidad nacionalidad : service.listarNacionalidadActiva()) {
            service.eliminarNacionalidad(nacionalidad.getId());
        }
        mvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay registros para mostrar.")));
    }

    // Obtiene el token del formulario renderizado: prueba también la integración Thymeleaf/Security.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session((MockHttpSession) pagina.getRequest().getSession())
                .param(token.getParameterName(), token.getToken());
    }
}
