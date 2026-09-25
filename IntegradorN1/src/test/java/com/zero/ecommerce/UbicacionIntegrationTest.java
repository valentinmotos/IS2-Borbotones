package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.repositories.LocalidadRepository;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class UbicacionIntegrationTest {

    private static final String BASE = "/admin/configuracion/ubicacion";
    private static final String LOCALIDADES = BASE + "/localidades";
    private final MockMvc mvc;
    private final PaisService paisService;
    private final ProvinciaService provinciaService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;
    private final LocalidadRepository localidadRepository;
    private MockHttpSession sesionJefe;

    UbicacionIntegrationTest(@Autowired MockMvc mvc, @Autowired PaisService paisService,
            @Autowired ProvinciaService provinciaService, @Autowired DepartamentoService departamentoService,
            @Autowired LocalidadService localidadService, @Autowired LocalidadRepository localidadRepository) {
        this.mvc = mvc;
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
        this.localidadRepository = localidadRepository;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesionJefe = login("jefe@zero.com.ar", "Jefe123!");
    }

    @Test
    void seederCargaArgentinaSusProvinciasYLosDepartamentosDeMendoza() throws Exception {
        Pais argentina = paisService.buscarPaisPorNombre("Argentina");
        assertThat(provinciaService.listarProvinciaActivo(argentina.getId())).hasSize(24);
        Provincia mendoza = provinciaService.buscarProvinciaPorNombre("Mendoza");
        assertThat(departamentoService.listarDepartamentoActivo(mendoza.getId())).hasSize(18);
        Departamento maipu = departamentoService.buscarDepartamentoPorNombre("Maipú");
        assertThat(localidadService.listarLocalidadActivo(maipu.getId()))
                .extracting(Localidad::getNombre, Localidad::getCodigoPostal)
                .contains(org.assertj.core.api.Assertions.tuple("Gutiérrez", "5511"));
    }

    @Test
    void creaUnaLocalidadEligiendoPaisProvinciaYDepartamento() throws Exception {
        Departamento maipu = departamentoService.buscarDepartamentoPorNombre("Maipú");
        mvc.perform(get(LOCALIDADES + "/nueva").session(sesionJefe))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<optgroup label=\"Argentina / Mendoza\">")))
                .andExpect(content().string(containsString("value=\"" + maipu.getId() + "\"")));

        mvc.perform(postConCsrf(LOCALIDADES, LOCALIDADES + "/nueva").param("nombre", " Fray Luis Beltrán ")
                .param("codigoPostal", "5531").param("departamentoId", maipu.getId()))
                .andExpect(redirectedUrl(LOCALIDADES + "?departamento=" + maipu.getId()))
                .andExpect(flash().attribute("exito", "Localidad creada correctamente."));

        Localidad creada = localidadService.buscarLocalidadPorNombre("Fray Luis Beltrán");
        assertThat(creada.getDepartamento().getId()).isEqualTo(maipu.getId());
        mvc.perform(get(LOCALIDADES).param("departamento", maipu.getId()).session(sesionJefe))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fray Luis Beltrán")))
                .andExpect(content().string(containsString("Gutiérrez")))
                .andExpect(content().string(not(containsString("Chacras de Coria"))))
                .andExpect(content().string(containsString("data-target=\"#eliminar-" + creada.getId() + "\"")));
    }

    @Test
    void duplicadoVuelveAlFormularioConElMensajeYLosDatos() throws Exception {
        Departamento maipu = departamentoService.buscarDepartamentoPorNombre("Maipú");
        long cantidad = localidadRepository.count();
        MvcResult resultado = mvc.perform(postConCsrf(LOCALIDADES, LOCALIDADES + "/nueva")
                .param("nombre", "GUTIERREZ").param("codigoPostal", "5511").param("departamentoId", maipu.getId()))
                .andExpect(redirectedUrl(LOCALIDADES + "/nueva"))
                .andExpect(flash().attribute("error",
                        "Ya existe una localidad con ese nombre en el departamento seleccionado."))
                .andReturn();
        mvc.perform(get(LOCALIDADES + "/nueva").session(sesionJefe).flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("value=\"GUTIERREZ\"")))
                .andExpect(content().string(containsString("value=\"" + maipu.getId() + "\" selected")));
        assertThat(localidadRepository.count()).isEqualTo(cantidad);
    }

    @Test
    void editaYDaDeBajaLogicaUnaLocalidad() throws Exception {
        Localidad russell = localidadService.buscarLocalidadPorNombre("Russell");
        String editar = LOCALIDADES + "/" + russell.getId() + "/editar";
        mvc.perform(get(editar).session(sesionJefe))
                .andExpect(content().string(containsString("value=\"Russell\"")))
                .andExpect(content().string(containsString("value=\"5517\"")));
        mvc.perform(postConCsrf(editar, editar).param("nombre", "Russell").param("codigoPostal", "M5517ABC")
                .param("departamentoId", russell.getDepartamento().getId()))
                .andExpect(flash().attribute("exito", "Localidad modificada correctamente."));
        assertThat(localidadService.buscarLocalidad(russell.getId()).getCodigoPostal()).isEqualTo("M5517ABC");

        mvc.perform(postConCsrf(LOCALIDADES + "/" + russell.getId() + "/eliminar", LOCALIDADES))
                .andExpect(flash().attribute("exito", "Localidad eliminada correctamente."));
        assertThat(localidadRepository.findById(russell.getId()).orElseThrow().isEliminado()).isTrue();
        mvc.perform(get(editar).session(sesionJefe)).andExpect(status().isNotFound());
    }

    @Test
    void noSeEliminaUnDepartamentoConLocalidadesActivas() throws Exception {
        Departamento maipu = departamentoService.buscarDepartamentoPorNombre("Maipú");
        String departamentos = BASE + "/departamentos";
        mvc.perform(postConCsrf(departamentos + "/" + maipu.getId() + "/eliminar", departamentos))
                .andExpect(flash().attribute("error",
                        "No se puede eliminar el departamento porque tiene localidades activas."));
        assertThat(departamentoService.buscarDepartamento(maipu.getId()).isEliminado()).isFalse();
    }

    @Test
    void provinciaDuplicadaEnElMismoPaisSeRechaza() throws Exception {
        Pais argentina = paisService.buscarPaisPorNombre("Argentina");
        String provincias = BASE + "/provincias";
        mvc.perform(postConCsrf(provincias, provincias + "/nueva").param("nombre", "cordoba")
                .param("paisId", argentina.getId()))
                .andExpect(redirectedUrl(provincias + "/nueva"))
                .andExpect(flash().attribute("error", "Ya existe una provincia con ese nombre en el país seleccionado."));
    }

    @Test
    void menuYPestanasLlevanALasCuatroPantallas() throws Exception {
        mvc.perform(get(BASE).session(sesionJefe)).andExpect(redirectedUrl(BASE + "/paises"));
        mvc.perform(get(BASE + "/paises").session(sesionJefe))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Argentina")))
                .andExpect(content().string(containsString("href=\"" + BASE + "/provincias\"")))
                .andExpect(content().string(containsString("href=\"" + BASE + "/departamentos\"")))
                .andExpect(content().string(containsString("href=\"" + LOCALIDADES + "\"")));
    }

    @Test
    void soloElJefeAccedeALaUbicacion() throws Exception {
        MockHttpSession administrativo = login("admin@zero.com.ar", "Admin123!");
        mvc.perform(get(LOCALIDADES).session(administrativo)).andExpect(status().isForbidden());
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
