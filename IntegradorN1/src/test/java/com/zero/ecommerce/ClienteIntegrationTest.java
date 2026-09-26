package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.enums.Sexo;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.NacionalidadService;
import com.zero.ecommerce.services.UsuarioService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ClienteIntegrationTest {

    private static final String BASE = "/cliente/perfil";
    private static final String CORREO = "cliente@zero.com.ar";
    private final MockMvc mvc;
    private final ClienteService service;
    private final UsuarioService usuarioService;
    private final LocalidadService localidadService;
    private final NacionalidadService nacionalidadService;
    private MockHttpSession sesion;

    ClienteIntegrationTest(@Autowired MockMvc mvc, @Autowired ClienteService service,
            @Autowired UsuarioService usuarioService, @Autowired LocalidadService localidadService,
            @Autowired NacionalidadService nacionalidadService) {
        this.mvc = mvc;
        this.service = service;
        this.usuarioService = usuarioService;
        this.localidadService = localidadService;
        this.nacionalidadService = nacionalidadService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        // Cliente activo que crea el seeder.
        sesion = login(CORREO, "Cliente123!");
    }

    @Test
    void cargaSuPerfilLoEditaYLaDireccionQuedaEnLaLocalidadCorrecta() throws Exception {
        mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Completá tus datos")))
                .andExpect(content().string(containsString("data-ubicacion")));

        Localidad chacras = localidadService.buscarLocalidadPorNombre("Chacras de Coria");
        mvc.perform(perfil(chacras).file(foto()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Tus datos se guardaron correctamente."));

        Cliente cliente = clienteLogueado();
        assertThat(cliente.getNombre()).isEqualTo("Ana");
        assertThat(cliente.getSexo()).isEqualTo(Sexo.FEMENINO);
        assertThat(cliente.getNumeroDocumento()).isEqualTo("30111222");
        assertThat(cliente.getNacionalidad().getNombre()).isEqualTo("Argentina");
        assertThat(cliente.getTelefono().getTipoTelefono()).isEqualTo(TipoTelefono.CELULAR);
        assertThat(cliente.getDireccion().getLocalidad().getId()).isEqualTo(chacras.getId());
        assertThat(cliente.getImagen()).isNotNull();

        // Al volver, el formulario muestra lo guardado, con la cascada precargada hasta la localidad.
        mvc.perform(get(BASE).session(sesion))
                .andExpect(content().string(containsString("value=\"Ana\"")))
                .andExpect(content().string(containsString("value=\"2611234567\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + chacras.getId() + "\"")))
                .andExpect(content().string(containsString("/imagen/" + cliente.getImagen().getId())));

        // Editar: cambia datos y localidad, sin crear otro cliente ni otra dirección, y conserva la foto.
        Localidad godoyCruz = localidadService.buscarLocalidadPorNombre("Godoy Cruz");
        String direccionOriginal = cliente.getDireccion().getId();
        String fotoOriginal = cliente.getImagen().getId();
        mvc.perform(perfil(godoyCruz, "nombre", "Ana María", "calle", "San Martín"))
                .andExpect(flash().attribute("exito", "Tus datos se guardaron correctamente."));
        Cliente editado = clienteLogueado();
        assertThat(editado.getId()).isEqualTo(cliente.getId());
        assertThat(editado.getNombre()).isEqualTo("Ana María");
        assertThat(editado.getDireccion().getId()).isEqualTo(direccionOriginal);
        assertThat(editado.getDireccion().getCalle()).isEqualTo("San Martín");
        assertThat(editado.getDireccion().getLocalidad().getId()).isEqualTo(godoyCruz.getId());
        assertThat(editado.getImagen().getId()).isEqualTo(fotoOriginal);
        assertThat(service.listarClienteActivo()).hasSize(1);
        // El header muestra el nombre del perfil.
        mvc.perform(get("/").session(sesion)).andExpect(content().string(containsString("Ana María Pérez")));
    }

    @Test
    void rechazaMenoresYDocumentoRepetidoYConservaLoCargado() throws Exception {
        Localidad localidad = localidadService.buscarLocalidadPorNombre("Ciudad de Mendoza");
        MvcResult error = mvc.perform(perfil(localidad, "fechaNacimiento", "2015-01-01"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("error", "Tenés que ser mayor de edad para registrar tu perfil."))
                .andReturn();
        assertThat(service.buscarClientePorUsuario(usuarioService.buscarUsuarioPorNombre(CORREO).getId())).isEmpty();
        mvc.perform(get(BASE).session(sesion).flashAttrs(error.getFlashMap()))
                .andExpect(content().string(containsString("value=\"Ana\"")))
                .andExpect(content().string(containsString("value=\"Belgrano\"")))
                .andExpect(content().string(containsString("data-seleccionado=\"" + localidad.getId() + "\"")));

        // Otro cliente ya tiene el DNI 30.111.222 (se compara sin puntos).
        service.crearCliente("Otra", "Persona", Sexo.OTRO, LocalDate.of(1990, 1, 1),
                TipoDocumento.DNI, "30111222", "2615550000",
                direccion(localidad), nacionalidadService.buscarNacionalidadPorNombre("Argentina").getId(), null);
        mvc.perform(perfil(localidad, "numeroDocumento", "30.111.222"))
                .andExpect(flash().attribute("error", "Ya hay otra persona registrada con ese documento."));
        mvc.perform(perfil(localidad, "sexo", ""))
                .andExpect(flash().attribute("error", "El sexo es obligatorio."));
    }

    @Test
    void soloLoVeUnCliente() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().is3xxRedirection());
        MockHttpSession admin = login("admin@zero.com.ar", "Admin123!");
        mvc.perform(get(BASE).session(admin)).andExpect(status().isForbidden());
    }

    private Cliente clienteLogueado() throws Exception {
        String idUsuario = usuarioService.buscarUsuarioPorNombre(CORREO).getId();
        return service.buscarClientePorUsuario(idUsuario).orElseThrow();
    }

    // Perfil completo y válido. cambios: pares nombre, valor que reemplazan a los de por defecto.
    private MockMultipartHttpServletRequestBuilder perfil(Localidad localidad, String... cambios) throws Exception {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("nombre", "Ana");
        datos.put("apellido", "Pérez");
        datos.put("sexo", "FEMENINO");
        datos.put("fechaNacimiento", "1995-05-10");
        datos.put("tipoDocumento", "DNI");
        datos.put("numeroDocumento", "30111222");
        datos.put("nacionalidadId", nacionalidadService.buscarNacionalidadPorNombre("Argentina").getId());
        datos.put("telefono", "2611234567");
        datos.put("calle", "Belgrano");
        datos.put("numeracion", "100");
        datos.put("localidadId", localidad.getId());
        for (int i = 0; i < cambios.length; i += 2) {
            datos.put(cambios[i], cambios[i + 1]);
        }
        MvcResult pagina = mvc.perform(get(BASE).session(sesion)).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        MockMultipartHttpServletRequestBuilder request = multipart(BASE);
        request.session(sesion).param(token.getParameterName(), token.getToken());
        datos.forEach(request::param);
        return request;
    }

    private DireccionForm direccion(Localidad localidad) {
        DireccionForm direccion = new DireccionForm();
        direccion.setCalle("Mitre");
        direccion.setNumeracion("50");
        direccion.setLocalidadId(localidad.getId());
        return direccion;
    }

    private MockMultipartFile foto() throws Exception {
        try (InputStream contenido = getClass().getResourceAsStream("/test-image.png")) {
            return new MockMultipartFile("foto", "foto.png", "image/png", contenido.readAllBytes());
        }
    }

    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession nueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(nueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave));
        return nueva;
    }
}
