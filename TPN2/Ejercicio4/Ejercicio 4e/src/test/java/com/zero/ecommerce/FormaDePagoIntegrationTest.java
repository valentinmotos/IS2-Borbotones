package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.config.DataSeeder;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.repositories.FormaDePagoRepository;
import com.zero.ecommerce.services.FormaDePagoService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class FormaDePagoIntegrationTest {

    private static final String BASE = "/admin/configuracion/formas-pago";
    private final MockMvc mvc;
    private final FormaDePagoService service;
    private final FormaDePagoRepository repository;
    private final DataSeeder seeder;
    private MockHttpSession sesionJefe;

    FormaDePagoIntegrationTest(@Autowired MockMvc mvc, @Autowired FormaDePagoService service,
            @Autowired FormaDePagoRepository repository, @Autowired DataSeeder seeder) {
        this.mvc = mvc;
        this.service = service;
        this.repository = repository;
        this.seeder = seeder;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesionJefe = login("jefe@zero.com.ar", "Jefe123!");
    }

    @Test
    void recorreAltaEdicionYBajaLogicaConFormulariosReales() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("tipoPago", "BILLETERA_VIRTUAL")
                .param("observacion", " Ualá "))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Forma de pago creada correctamente."));
        FormaDePago creada = activa(TipoPago.BILLETERA_VIRTUAL, "Ualá");
        String id = creada.getId();
        mvc.perform(get(BASE).session(sesionJefe))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Billetera virtual")))
                .andExpect(content().string(containsString(BASE + "/" + id + "/editar")))
                .andExpect(content().string(containsString("data-target=\"#eliminar-" + id + "\"")))
                .andExpect(content().string(containsString("action=\"" + BASE + "/" + id + "/eliminar\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        String editar = BASE + "/" + id + "/editar";
        mvc.perform(get(editar).session(sesionJefe))
                .andExpect(content().string(containsString("value=\"Ualá\"")))
                .andExpect(content().string(containsString("value=\"BILLETERA_VIRTUAL\" selected")));
        mvc.perform(postConCsrf(editar, editar).param("tipoPago", "TRANSFERENCIA")
                .param("observacion", "Banco Nación"))
                .andExpect(redirectedUrl(BASE));
        FormaDePago modificada = service.buscarFormaDePago(id);
        assertThat(modificada.getTipoPago()).isEqualTo(TipoPago.TRANSFERENCIA);
        assertThat(modificada.getObservacion()).isEqualTo("Banco Nación");
        mvc.perform(postConCsrf(BASE + "/" + id + "/eliminar", BASE))
                .andExpect(redirectedUrl(BASE));
        assertThat(repository.findById(id).orElseThrow().isEliminado()).isTrue();
        assertThat(service.listarFormaDePagoActivo()).extracting(FormaDePago::getId).doesNotContain(id);
        assertThat(service.listarFormaDePago()).extracting(FormaDePago::getId).contains(id);
        mvc.perform(get(editar).session(sesionJefe)).andExpect(status().isNotFound());
        mvc.perform(postConCsrf(editar, BASE + "/nueva").param("tipoPago", "EFECTIVO")
                .param("observacion", "Otra"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicadoVuelveConFlashYConservaLosDatos() throws Exception {
        long cantidad = repository.count();
        MvcResult resultado = mvc.perform(postConCsrf(BASE, BASE + "/nueva")
                .param("tipoPago", "BILLETERA_VIRTUAL").param("observacion", " MERCADO PAGO "))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "Ya existe una forma de pago activa con ese tipo y observación."))
                .andExpect(flash().attribute("observacion", " MERCADO PAGO ")).andReturn();
        mvc.perform(get(BASE + "/nueva").session(sesionJefe).flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("Ya existe una forma de pago activa con ese tipo y observación.")))
                .andExpect(content().string(containsString("value=\" MERCADO PAGO \"")))
                .andExpect(content().string(containsString("value=\"BILLETERA_VIRTUAL\" selected")));
        assertThat(repository.count()).isEqualTo(cantidad);
    }

    @Test
    void desactivadaPuedeVolverADarseDeAlta() throws Exception {
        FormaDePago efectivo = activa(TipoPago.EFECTIVO, "Efectivo");
        service.eliminarFormaDePago(efectivo.getId());
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("tipoPago", "EFECTIVO")
                .param("observacion", "Efectivo"))
                .andExpect(redirectedUrl(BASE));
        assertThat(activa(TipoPago.EFECTIVO, "Efectivo").getId()).isNotEqualTo(efectivo.getId());
    }

    @Test
    void validaCamposEnServidor() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("tipoPago", "").param("observacion", "Efectivo"))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "El tipo de pago es obligatorio."));
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("tipoPago", "CHEQUE").param("observacion", "Cheque"))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "El tipo de pago no es válido."));
        mvc.perform(postConCsrf(BASE, BASE + "/nueva").param("tipoPago", "EFECTIVO").param("observacion", " "))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error", "La observación de la forma de pago es obligatoria."));
    }

    @Test
    void rechazaMutacionesSinCsrfYBajaPorGet() throws Exception {
        String id = activa(TipoPago.EFECTIVO, "Efectivo").getId();
        mvc.perform(post(BASE).session(sesionJefe).param("tipoPago", "EFECTIVO").param("observacion", "Otra"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + id + "/editar").session(sesionJefe).param("tipoPago", "EFECTIVO")
                .param("observacion", "Otra"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + id + "/eliminar").session(sesionJefe)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + id + "/eliminar").session(sesionJefe)).andExpect(status().isMethodNotAllowed());
        assertThat(service.buscarFormaDePago(id).isEliminado()).isFalse();
        mvc.perform(get(BASE + "/inexistente/editar").session(sesionJefe)).andExpect(status().isNotFound());
    }

    @Test
    void soloElJefeAccedeALaConfiguracion() throws Exception {
        mvc.perform(get(BASE)).andExpect(redirectedUrl("http://localhost/login"));
        MockHttpSession administrativo = login("admin@zero.com.ar", "Admin123!");
        mvc.perform(get(BASE).session(administrativo)).andExpect(status().isForbidden());
    }

    @Test
    void seederCargaLasTresFormasDePagoYNoLasDuplica() throws Exception {
        assertThat(service.listarFormaDePagoActivo())
                .extracting(FormaDePago::getTipoPago, FormaDePago::getObservacion)
                .contains(tuple(TipoPago.EFECTIVO, "Efectivo"),
                        tuple(TipoPago.TRANSFERENCIA, "Transferencia"),
                        tuple(TipoPago.BILLETERA_VIRTUAL, "Mercado Pago"));
        long cantidad = repository.count();
        seeder.run();
        assertThat(repository.count()).isEqualTo(cantidad);
    }

    @Test
    void listadoVacioMuestraMensaje() throws Exception {
        for (FormaDePago formaDePago : service.listarFormaDePagoActivo()) {
            service.eliminarFormaDePago(formaDePago.getId());
        }
        assertThat(service.listarFormaDePagoActivo()).isEmpty();
        mvc.perform(get(BASE).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay registros para mostrar.")));
    }

    private FormaDePago activa(TipoPago tipoPago, String observacion) {
        return service.listarFormaDePagoActivo().stream()
                .filter(f -> f.getTipoPago() == tipoPago && f.getObservacion().equals(observacion))
                .findFirst().orElseThrow();
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesion;
    }

    // Obtiene el token del formulario renderizado: prueba también la integración Thymeleaf/Security.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesionJefe).param(token.getParameterName(), token.getToken());
    }
}
