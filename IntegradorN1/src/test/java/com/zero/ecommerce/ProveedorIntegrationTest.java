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

import java.util.List;

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

import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.repositories.ProveedorRepository;
import com.zero.ecommerce.services.ProveedorService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ProveedorIntegrationTest {

    private static final String BASE = "/admin/proveedores";
    private final MockMvc mvc;
    private final ProveedorService proveedorService;
    private final ProveedorRepository proveedorRepository;
    private MockHttpSession sesion;

    ProveedorIntegrationTest(@Autowired MockMvc mvc, @Autowired ProveedorService proveedorService,
            @Autowired ProveedorRepository proveedorRepository) {
        this.mvc = mvc;
        this.proveedorService = proveedorService;
        this.proveedorRepository = proveedorRepository;
    }

    // El panel de proveedores lo usan JEFE y ADMINISTRATIVO: se prueba con el administrativo.
    @BeforeEach
    void iniciarSesion() throws Exception {
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void seederCargaCuatroProveedoresYElListadoTieneElBotonDeWhatsApp() throws Exception {
        List<Proveedor> proveedores = proveedorService.listarProveedorActivo();
        assertThat(proveedores).hasSize(4);
        assertThat(proveedores).allMatch(p -> !p.listarCorreoActivo().isEmpty() && p.buscarCelularActivo().isPresent());

        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Distribuidora Deportiva Cuyo S.A.")))
                .andExpect(content().string(containsString("href=\"https://wa.me/5492614123456\"")))
                .andExpect(content().string(containsString("4 proveedores")));
    }

    @Test
    void elBuscadorFiltraPorRazonSocial() throws Exception {
        mvc.perform(get(BASE).param("buscar", "andina").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Accesorios Fitness Andina")))
                .andExpect(content().string(not(containsString("Distribuidora Deportiva Cuyo S.A."))))
                .andExpect(content().string(containsString("1 proveedor")));
    }

    @Test
    void creaUnProveedorDesdeElFormularioYElWhatsAppAbreElNumeroCargado() throws Exception {
        mvc.perform(get(BASE + "/nuevo").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"plantilla-contacto\"")))
                .andExpect(content().string(containsString("name=\"contactos[INDICE].tipo\"")))
                .andExpect(content().string(containsString("name=\"contactos[1].tipo\"")))
                .andExpect(content().string(containsString("/js/proveedor-contactos.js")));

        mvc.perform(postConCsrf(BASE, BASE + "/nuevo").param("razonSocial", "Running Mendoza")
                .param("contactos[0].tipo", "CORREO").param("contactos[0].valor", "Ventas@RunningMza.com.ar")
                .param("contactos[0].tipoContacto", "EMPRESA").param("contactos[0].observacion", "Ventas")
                .param("contactos[1].tipo", "CELULAR").param("contactos[1].valor", "+54 9 261 555-1234")
                .param("contactos[1].tipoContacto", "LABORAL"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Proveedor creado correctamente."));

        Proveedor proveedor = buscar("Running Mendoza");
        assertThat(proveedor.listarCorreoActivo()).extracting(ContactoCorreoElectronico::getEmail)
                .containsExactly("ventas@runningmza.com.ar");
        assertThat(proveedor.buscarCelularActivo()).get().extracting(ContactoTelefonico::getTelefono)
                .isEqualTo("5492615551234");
        mvc.perform(get(BASE).param("buscar", "Running").session(sesion))
                .andExpect(content().string(containsString("href=\"https://wa.me/5492615551234\"")));
    }

    @Test
    void unProveedorSinCelularSeRechazaYVuelveConLosDatos() throws Exception {
        long cantidad = proveedorRepository.count();
        MvcResult resultado = mvc.perform(postConCsrf(BASE, BASE + "/nuevo").param("razonSocial", "Sin Celular S.A.")
                .param("contactos[0].tipo", "CORREO").param("contactos[0].valor", "ventas@sincelular.com")
                .param("contactos[0].tipoContacto", "EMPRESA")
                .param("contactos[1].tipo", "FIJO").param("contactos[1].valor", "261 423-0000")
                .param("contactos[1].tipoContacto", "EMPRESA"))
                .andExpect(redirectedUrl(BASE + "/nuevo"))
                .andExpect(flash().attribute("error",
                        "El proveedor tiene que tener al menos un teléfono celular para WhatsApp."))
                .andReturn();
        mvc.perform(get(BASE + "/nuevo").session(sesion).flashAttrs(resultado.getFlashMap()))
                .andExpect(content().string(containsString("value=\"Sin Celular S.A.\"")))
                .andExpect(content().string(containsString("value=\"ventas@sincelular.com\"")))
                .andExpect(content().string(containsString("value=\"261 423-0000\"")));
        assertThat(proveedorRepository.count()).isEqualTo(cantidad);
    }

    @Test
    void editarModificaLosContactosSinDuplicarlos() throws Exception {
        Proveedor proveedor = buscar("Calzados y Textiles del Plata S.R.L.");
        ContactoCorreoElectronico correo = proveedor.listarCorreoActivo().get(0);
        ContactoTelefonico celular = proveedor.buscarCelularActivo().orElseThrow();
        String editar = BASE + "/" + proveedor.getId() + "/editar";

        mvc.perform(get(editar).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Calzados y Textiles del Plata S.R.L.\"")))
                .andExpect(content().string(containsString("value=\"" + correo.getId() + "\"")));

        // Cambia el correo, deja el celular como estaba y agrega un teléfono fijo.
        mvc.perform(postConCsrf(editar, editar).param("razonSocial", "Textiles del Plata S.R.L.")
                .param("contactos[0].id", correo.getId()).param("contactos[0].tipo", "CORREO")
                .param("contactos[0].valor", "compras@textilesdelplata.com").param("contactos[0].tipoContacto", "EMPRESA")
                .param("contactos[1].id", celular.getId()).param("contactos[1].tipo", "CELULAR")
                .param("contactos[1].valor", celular.getTelefono()).param("contactos[1].tipoContacto", "LABORAL")
                .param("contactos[2].tipo", "FIJO").param("contactos[2].valor", "011 4555-1234")
                .param("contactos[2].tipoContacto", "EMPRESA"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Proveedor modificado correctamente."));

        Proveedor modificado = proveedorService.buscarProveedor(proveedor.getId());
        assertThat(modificado.getRazonSocial()).isEqualTo("Textiles del Plata S.R.L.");
        assertThat(modificado.getContactos()).hasSize(3).noneMatch(c -> c.isEliminado());
        assertThat(modificado.listarCorreoActivo()).singleElement()
                .satisfies(c -> assertThat(c.getId()).isEqualTo(correo.getId()))
                .extracting(ContactoCorreoElectronico::getEmail).isEqualTo("compras@textilesdelplata.com");
        assertThat(modificado.listarTelefonoActivo()).extracting(ContactoTelefonico::getTipoTelefono)
                .containsExactlyInAnyOrder(TipoTelefono.CELULAR, TipoTelefono.FIJO);
    }

    @Test
    void quitarUnaFilaDaDeBajaEseContacto() throws Exception {
        Proveedor proveedor = buscar("Accesorios Fitness Andina");
        List<ContactoCorreoElectronico> correos = proveedor.listarCorreoActivo();
        assertThat(correos).hasSize(2);
        ContactoTelefonico celular = proveedor.buscarCelularActivo().orElseThrow();
        String editar = BASE + "/" + proveedor.getId() + "/editar";

        mvc.perform(postConCsrf(editar, editar).param("razonSocial", proveedor.getRazonSocial())
                .param("contactos[0].id", correos.get(0).getId()).param("contactos[0].tipo", "CORREO")
                .param("contactos[0].valor", correos.get(0).getEmail()).param("contactos[0].tipoContacto", "EMPRESA")
                .param("contactos[1].id", celular.getId()).param("contactos[1].tipo", "CELULAR")
                .param("contactos[1].valor", celular.getTelefono()).param("contactos[1].tipoContacto", "LABORAL"))
                .andExpect(flash().attribute("exito", "Proveedor modificado correctamente."));

        Proveedor modificado = proveedorService.buscarProveedor(proveedor.getId());
        assertThat(modificado.listarCorreoActivo()).extracting(ContactoCorreoElectronico::getId)
                .containsExactly(correos.get(0).getId());
        assertThat(correos.get(1).isEliminado()).isTrue();
    }

    @Test
    void eliminarDaDeBajaElProveedorYSusContactos() throws Exception {
        Proveedor proveedor = buscar("Indumentaria Atlética San Juan");
        mvc.perform(postConCsrf(BASE + "/" + proveedor.getId() + "/eliminar", BASE))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Proveedor eliminado correctamente."));
        Proveedor eliminado = proveedorRepository.findById(proveedor.getId()).orElseThrow();
        assertThat(eliminado.isEliminado()).isTrue();
        assertThat(eliminado.getContactos()).isNotEmpty().allMatch(c -> c.isEliminado());
        mvc.perform(get(BASE + "/" + proveedor.getId() + "/editar").session(sesion)).andExpect(status().isNotFound());
    }

    private Proveedor buscar(String razonSocial) {
        return proveedorService.listarProveedorActivo(razonSocial).get(0);
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesionNueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesionNueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesionNueva;
    }

    // Obtiene el token del formulario renderizado: prueba también que la página se renderiza sin errores.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }
}
