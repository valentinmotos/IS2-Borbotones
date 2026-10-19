package ar.edu.clinicaweb;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties={"debug=false","logging.level.root=WARN"})
@AutoConfigureMockMvc
class FrontendIntegrationTest {
    @Autowired MockMvc mvc;
    static final HttpServer api;
    static final String PERSONA="{\"id\":\"p1\",\"nombre\":\"Ana\",\"apellido\":\"García\",\"documento\":\"12345678\",\"foto\":null}";
    static final String USUARIO="{\"id\":\"u1\",\"nombreUsuario\":\"demo\"}";
    static final String HISTORIA="{\"id\":\"h1\",\"paciente\":"+PERSONA+",\"usuario\":"+USUARIO+"}";
    static final String DETALLE="{\"id\":\"d1\",\"fechaHistoria\":\"2026-01-01\",\"detalleHistoria\":\"Atención de prueba\",\"medico\":"+PERSONA+"}";
    static {
        try {
            api=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            api.createContext("/api",exchange->{
                String ruta=exchange.getRequestURI().getPath();
                String body=switch(ruta) {
                    case "/api/pacientes", "/api/medicos" -> "["+PERSONA+"]";
                    case "/api/usuarios" -> "["+USUARIO+"]";
                    case "/api/historias" -> "["+HISTORIA+"]";
                    case "/api/pacientes/p1", "/api/medicos/p1" -> PERSONA;
                    case "/api/usuarios/u1" -> USUARIO;
                    case "/api/historias/h1/detalles/d1" -> DETALLE;
                    default -> "{\"historia\":"+HISTORIA+",\"atenciones\":["+DETALLE+"],\"totalAtenciones\":1}";
                };
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            });
            api.start();
        } catch(Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void configurar(DynamicPropertyRegistry registry) { registry.add("clinica.api",()->"http://127.0.0.1:"+api.getAddress().getPort()+"/api"); }
    @AfterAll static void cerrar() { api.stop(0); }
    private MockHttpSession sesion() { var s=new MockHttpSession(); s.setAttribute("usuario","demo"); s.setAttribute("clave","clave-test"); s.setAttribute("csrf","token-test"); return s; }

    @Test void exigeSesionYProtegePostConCsrf() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Tu espacio de atención")));
        mvc.perform(post("/guardar/pacientes").session(sesion())).andExpect(status().isForbidden());
    }
    @Test void renderizaTodosLosCatalogosYFormulariosDeEdicion() throws Exception {
        for (String vista:new String[]{"pacientes","medicos","usuarios","historias"}) {
            String id=vista.equals("usuarios")?"u1":vista.equals("historias")?"h1":"p1";
            mvc.perform(get("/").param("vista",vista).session(sesion())).andExpect(status().isOk()).andExpect(view().name("panel")).andExpect(content().string(containsString("Nuevo registro")));
            mvc.perform(get("/").param("vista",vista).param("editar",id).session(sesion())).andExpect(status().isOk()).andExpect(content().string(containsString("Editar registro")));
        }
    }
    @Test void renderizaExpedienteYEdicionDeAtencion() throws Exception {
        mvc.perform(get("/").param("historia","h1").param("detalleEditar","d1").session(sesion()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Atención de prueba"))).andExpect(content().string(containsString("Editar atención")));
    }
    @Test void loginConsumeApiYCierraSesion() throws Exception {
        var s=sesion();
        mvc.perform(post("/login").session(s).param("csrf","token-test").param("usuario","demo").param("clave","clave-test"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(post("/salir").session(s).param("csrf","token-test")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
