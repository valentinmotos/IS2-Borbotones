package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.repositories.DireccionRepository;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.DireccionService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class DireccionIntegrationTest {

    private static final String API = "/api/ubicacion";
    private static final String DEV = "/dev/direccion";
    private final MockMvc mvc;
    private final PaisService paisService;
    private final ProvinciaService provinciaService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;
    private final DireccionService direccionService;
    private final DireccionRepository direccionRepository;

    DireccionIntegrationTest(@Autowired MockMvc mvc, @Autowired PaisService paisService,
            @Autowired ProvinciaService provinciaService, @Autowired DepartamentoService departamentoService,
            @Autowired LocalidadService localidadService, @Autowired DireccionService direccionService,
            @Autowired DireccionRepository direccionRepository) {
        this.mvc = mvc;
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
        this.direccionService = direccionService;
        this.direccionRepository = direccionRepository;
    }

    @Test
    void endpointsPublicosDevuelvenCadaNivelConIdYNombre() throws Exception {
        String argentina = paisService.buscarPaisPorNombre("Argentina").getId();
        Provincia mendoza = provinciaService.buscarProvinciaPorNombre("Mendoza");
        Departamento maipu = departamentoService.buscarDepartamentoPorNombre("Maipú");

        mvc.perform(get(API + "/paises")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Argentina"))
                .andExpect(jsonPath("$[0].id").value(argentina));
        mvc.perform(get(API + "/provincias").param("pais", argentina)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[*].nombre", hasItem("Mendoza")));
        mvc.perform(get(API + "/departamentos").param("provincia", mendoza.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[*].nombre", hasItem("Maipú")));
        mvc.perform(get(API + "/localidades").param("departamento", maipu.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Gutiérrez')].codigoPostal", hasItem("5511")));
    }

    @Test
    void idVacioOInexistenteDevuelveListaVacia() throws Exception {
        mvc.perform(get(API + "/provincias").param("pais", "")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(API + "/departamentos").param("provincia", "no-existe")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void noDevuelveLocalidadesEliminadas() throws Exception {
        Localidad russell = localidadService.buscarLocalidadPorNombre("Russell");
        localidadService.eliminarLocalidad(russell.getId());
        mvc.perform(get(API + "/localidades").param("departamento", russell.getDepartamento().getId()))
                .andExpect(jsonPath("$[*].nombre", org.hamcrest.Matchers.not(hasItem("Russell"))));
    }

    @Test
    void elFragmentCreaUnaDireccionYLaPrecargaAlEditar() throws Exception {
        Localidad gutierrez = localidadService.buscarLocalidadPorNombre("Gutiérrez");
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(get(DEV).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-ubicacion-cascada")))
                .andExpect(content().string(containsString("data-api=\"" + API + "\"")))
                .andExpect(content().string(containsString("/js/ubicacion-cascada.js")))
                .andExpect(content().string(containsString("data-ubicacion-cp")));

        MvcResult alta = mvc.perform(postConCsrf(DEV, DEV, sesion).param("calle", "Ozamis")
                .param("numeracion", "1250").param("barrio", "Centro").param("referencia", "Frente a la plaza")
                .param("localidadId", gutierrez.getId()))
                .andExpect(redirectedUrlPattern(DEV + "?id=*"))
                .andExpect(flash().attribute("exito", "Dirección creada correctamente."))
                .andReturn();
        String id = alta.getResponse().getRedirectedUrl().substring((DEV + "?id=").length());
        Direccion guardada = direccionService.buscarDireccion(id);
        assertThat(guardada.getLocalidad().getId()).isEqualTo(gutierrez.getId());
        assertThat(guardada.getManzanaPiso()).isNull();

        Departamento maipu = gutierrez.getDepartamento();
        mvc.perform(get(DEV).param("id", id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Ozamis\"")))
                .andExpect(content().string(containsString("value=\"Frente a la plaza\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + gutierrez.getId() + "\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + maipu.getId() + "\"")))
                .andExpect(content().string(containsString(
                        "data-seleccionado=\"" + maipu.getProvincia().getId() + "\"")))
                .andExpect(content().string(containsString(
                        "data-seleccionado=\"" + maipu.getProvincia().getPais().getId() + "\"")));

        mvc.perform(postConCsrf(DEV + "?id=" + id, DEV + "?id=" + id, sesion).param("calle", "Ozamis")
                .param("numeracion", "1300").param("localidadId", gutierrez.getId()))
                .andExpect(redirectedUrl(DEV + "?id=" + id))
                .andExpect(flash().attribute("exito", "Dirección modificada correctamente."));
        assertThat(direccionService.buscarDireccion(id).getNumeracion()).isEqualTo("1300");
    }

    @Test
    void unErrorVuelveAlFormularioConLosDatosYLosSelectsCargados() throws Exception {
        Localidad gutierrez = localidadService.buscarLocalidadPorNombre("Gutiérrez");
        MockHttpSession sesion = new MockHttpSession();
        long cantidad = direccionRepository.count();
        MvcResult resultado = mvc.perform(postConCsrf(DEV, DEV, sesion).param("calle", "Ozamis")
                .param("numeracion", "").param("localidadId", gutierrez.getId())
                .param("departamentoId", gutierrez.getDepartamento().getId()))
                .andExpect(redirectedUrl(DEV))
                .andExpect(flash().attribute("error", "La numeración es obligatoria."))
                .andReturn();
        mvc.perform(get(DEV).session(sesion).flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("La numeración es obligatoria.")))
                .andExpect(content().string(containsString("value=\"Ozamis\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + gutierrez.getId() + "\"")));
        assertThat(direccionRepository.count()).isEqualTo(cantidad);
    }

    // Obtiene el token del formulario renderizado: prueba también la integración Thymeleaf/Security.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario, MockHttpSession sesion)
            throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }
}
