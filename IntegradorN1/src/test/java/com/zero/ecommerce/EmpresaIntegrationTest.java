package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.enums.TipoEmpresa;
import com.zero.ecommerce.repositories.EmpresaRepository;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.LocalidadService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class EmpresaIntegrationTest {

    private static final String BASE = "/admin/configuracion/empresa";
    private final MockMvc mvc;
    private final EmpresaService empresaService;
    private final EmpresaRepository empresaRepository;
    private final LocalidadService localidadService;
    private MockHttpSession sesionJefe;

    EmpresaIntegrationTest(@Autowired MockMvc mvc, @Autowired EmpresaService empresaService,
            @Autowired EmpresaRepository empresaRepository, @Autowired LocalidadService localidadService) {
        this.mvc = mvc;
        this.empresaService = empresaService;
        this.empresaRepository = empresaRepository;
        this.localidadService = localidadService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesionJefe = login("jefe@zero.com.ar", "Jefe123!");
    }

    @Test
    void seederCargaZeroComoSedeCentralConDireccionYContactos() throws Exception {
        Empresa sede = empresaService.buscarSedeCentral();
        assertThat(sede.getRazonSocial()).isEqualTo("Zero Indumentaria Deportiva S.A.");
        assertThat(sede.getCuit()).isEqualTo("30-71567890-6");
        assertThat(sede.getDireccion().getLocalidad().getNombre()).isEqualTo("Ciudad de Mendoza");
        assertThat(sede.buscarCorreoActivo()).get().extracting("email").isEqualTo("contacto@zero.com.ar");
        assertThat(sede.buscarTelefonoActivo()).isPresent();
        mvc.perform(get(BASE).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Zero Indumentaria Deportiva S.A.")))
                .andExpect(content().string(containsString("Sede central")));
    }

    @Test
    void creaUnaSucursalConElFragmentDeDireccionYLaEdita() throws Exception {
        Localidad maipu = localidadService.buscarLocalidadPorNombre("Maipú");
        mvc.perform(get(BASE + "/nueva").session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-ubicacion-cascada")))
                .andExpect(content().string(containsString("name=\"razonSocial\"")));

        mvc.perform(conDatos(postConCsrf(BASE, BASE + "/nueva"), "Zero Maipú", "30712345671", maipu))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Empresa creada correctamente."));

        Empresa sucursal = empresaService.buscarEmpresaPorNombre("Zero Maipú");
        assertThat(sucursal.getCuit()).isEqualTo("30-71234567-1");
        assertThat(sucursal.getTipoSucursal()).isEqualTo(TipoEmpresa.SUCURSAL);
        assertThat(sucursal.getDireccion().getLocalidad().getId()).isEqualTo(maipu.getId());
        assertThat(sucursal.getContactos()).hasSize(2);

        String editar = BASE + "/" + sucursal.getId() + "/editar";
        mvc.perform(get(editar).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Zero Maipú\"")))
                .andExpect(content().string(containsString("value=\"maipu@zero.com.ar\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + maipu.getId() + "\"")));

        mvc.perform(conDatos(postConCsrf(editar, editar), "Zero Maipú Centro", "30-71234567-1", maipu,
                "maipu.centro@zero.com.ar"))
                .andExpect(flash().attribute("exito", "Empresa modificada correctamente."));
        Empresa modificada = empresaService.buscarEmpresa(sucursal.getId());
        assertThat(modificada.getRazonSocial()).isEqualTo("Zero Maipú Centro");
        assertThat(modificada.buscarCorreoActivo()).get().extracting("email").isEqualTo("maipu.centro@zero.com.ar");
        assertThat(modificada.getContactos()).hasSize(2);
    }

    @Test
    void cuitInvalidoVuelveAlFormularioConLosDatos() throws Exception {
        Localidad maipu = localidadService.buscarLocalidadPorNombre("Maipú");
        long cantidad = empresaRepository.count();
        MvcResult resultado = mvc.perform(conDatos(postConCsrf(BASE, BASE + "/nueva"), "Zero Maipú", "30-71234567-9",
                maipu))
                .andExpect(redirectedUrl(BASE + "/nueva"))
                .andExpect(flash().attribute("error",
                        "El CUIT no es válido: tiene que tener 11 dígitos (XX-XXXXXXXX-X) y un dígito verificador correcto."))
                .andReturn();
        mvc.perform(get(BASE + "/nueva").session(sesionJefe).flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("value=\"Zero Maipú\"")))
                .andExpect(content().string(containsString("value=\"Ozamis\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + maipu.getId() + "\"")));
        assertThat(empresaRepository.count()).isEqualTo(cantidad);
    }

    @Test
    void noSeCreaUnaSegundaSedeCentralNiSeEliminaLaExistente() throws Exception {
        Localidad maipu = localidadService.buscarLocalidadPorNombre("Maipú");
        mvc.perform(conDatos(postConCsrf(BASE, BASE + "/nueva"), "Otra sede", "30712345671", maipu,
                "otra@zero.com.ar", "SEDE_CENTRAL"))
                .andExpect(flash().attribute("error", "Ya existe una sede central. Solo puede haber una."));
        Empresa sede = empresaService.buscarSedeCentral();
        mvc.perform(postConCsrf(BASE + "/" + sede.getId() + "/eliminar", BASE))
                .andExpect(flash().attribute("error", "No se puede eliminar la sede central."));
        assertThat(empresaService.buscarSedeCentral().getId()).isEqualTo(sede.getId());
    }

    @Test
    void eliminarUnaSucursalDaDeBajaSuDireccionYContactos() throws Exception {
        Localidad maipu = localidadService.buscarLocalidadPorNombre("Maipú");
        mvc.perform(conDatos(postConCsrf(BASE, BASE + "/nueva"), "Zero Maipú", "30712345671", maipu));
        Empresa sucursal = empresaService.buscarEmpresaPorNombre("Zero Maipú");
        mvc.perform(postConCsrf(BASE + "/" + sucursal.getId() + "/eliminar", BASE))
                .andExpect(flash().attribute("exito", "Empresa eliminada correctamente."));
        Empresa eliminada = empresaRepository.findById(sucursal.getId()).orElseThrow();
        assertThat(eliminada.isEliminado()).isTrue();
        assertThat(eliminada.getDireccion().isEliminado()).isTrue();
        assertThat(eliminada.getContactos()).allMatch(c -> c.isEliminado());
    }

    @Test
    void soloElJefeAccedeALaEmpresa() throws Exception {
        MockHttpSession administrativo = login("admin@zero.com.ar", "Admin123!");
        mvc.perform(get(BASE).session(administrativo)).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder conDatos(MockHttpServletRequestBuilder post, String razonSocial,
            String cuit, Localidad localidad) {
        return conDatos(post, razonSocial, cuit, localidad, "maipu@zero.com.ar");
    }

    private MockHttpServletRequestBuilder conDatos(MockHttpServletRequestBuilder post, String razonSocial,
            String cuit, Localidad localidad, String correo) {
        return conDatos(post, razonSocial, cuit, localidad, correo, "SUCURSAL");
    }

    private MockHttpServletRequestBuilder conDatos(MockHttpServletRequestBuilder post, String razonSocial,
            String cuit, Localidad localidad, String correo, String tipo) {
        return post.param("razonSocial", razonSocial).param("cuit", cuit).param("tipoSucursal", tipo)
                .param("calle", "Ozamis").param("numeracion", "900").param("localidadId", localidad.getId())
                .param("departamentoId", localidad.getDepartamento().getId())
                .param("correo", correo).param("telefono", "261 497-0000").param("tipoTelefono", "FIJO");
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
