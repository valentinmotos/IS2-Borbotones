package ar.edu.clinica;

import ar.edu.clinica.api.Solicitudes.*;
import ar.edu.clinica.model.*;
import ar.edu.clinica.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:clinica-test;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","clinica.demo=false","debug=false","logging.level.root=WARN"})
@AutoConfigureMockMvc @Transactional
class ClinicaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioService usuarios;
    @Autowired PacienteService pacientes;
    @Autowired MedicoService medicos;
    @Autowired HistoriaService historias;
    @Autowired DetalleService detalles;
    @Autowired EntityManager em;
    Usuario usuario; Paciente paciente; Medico medico; HistoriaClinica historia;
    @BeforeEach void datos() {
        usuario=usuarios.crear(new Cuenta("prueba","Prueba123!"));
        paciente=pacientes.crear(new Persona("Ana","García","12345678"));
        medico=medicos.crear(new Persona("Luis","Pérez","87654321"));
        historia=historias.crear(new Historia(paciente.id,usuario.id));
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor auth() { return httpBasic("prueba","Prueba123!"); }
    private Detalle atencion(String tipo,String texto) { return new Detalle(LocalDate.now(),texto,medico.id,tipo); }

    @Test void autenticaYNoExponeClaves() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").with(httpBasic("prueba","incorrecta"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").with(auth())).andExpect(status().isOk()).andExpect(jsonPath("$[0].clave").doesNotExist());
        assertTrue(usuario.clave.startsWith("$2a$"));
    }
    @Test void crudPacienteValidaDuplicadosYConservaBaja() throws Exception {
        String contenido=mvc.perform(post("/api/pacientes").with(auth()).contentType("application/json").content(json.writeValueAsString(new Persona("Sol","Díaz","11111111"))))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn().getResponse().getContentAsString();
        String id=json.readTree(contenido).get("id").asText();
        mvc.perform(put("/api/pacientes/"+id).with(auth()).contentType("application/json").content(json.writeValueAsString(new Persona("Solana","Díaz","11111111"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Solana"));
        mvc.perform(delete("/api/pacientes/"+id).with(auth())).andExpect(status().isNoContent());
        mvc.perform(get("/api/pacientes/"+id).with(auth())).andExpect(status().isNotFound());
        assertTrue(em.find(Paciente.class,id).eliminado);
        mvc.perform(post("/api/pacientes").with(auth()).contentType("application/json").content(json.writeValueAsString(new Persona("Otra","Persona","11111111"))))
            .andExpect(status().isConflict());
    }
    @Test void validaCamposYReferencias() throws Exception {
        mvc.perform(post("/api/pacientes").with(auth()).contentType("application/json").content("{\"nombre\":\" \",\"apellido\":\"A\",\"documento\":\"B\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(post("/api/historias").with(auth()).contentType("application/json").content(json.writeValueAsString(new Historia("no-existe",usuario.id))))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/historias/"+historia.id+"/detalles").with(auth()).contentType("application/json").content(json.writeValueAsString(new Detalle(LocalDate.now().plusDays(1),"Detalle de prueba",medico.id,"GENERAL"))))
            .andExpect(status().isBadRequest());
    }
    @Test void strategyCambiaValidacionSinPersistirFallos() throws Exception {
        String url="/api/historias/"+historia.id+"/detalles";
        mvc.perform(post(url).with(auth()).contentType("application/json").content(json.writeValueAsString(atencion("CONTROL","Texto de quince"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post(url).with(auth()).contentType("application/json").content(json.writeValueAsString(atencion("DESCONOCIDO","Texto de quince"))))
            .andExpect(status().isBadRequest());
        assertEquals(0,detalles.deHistoria(historia.id).size());
        mvc.perform(post(url).with(auth()).contentType("application/json").content(json.writeValueAsString(atencion("GENERAL","Texto de quince"))))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/historias/"+historia.id).with(auth())).andExpect(status().isOk()).andExpect(jsonPath("$.totalAtenciones").value(1));
    }
    @Test void composicionDaDeBajaDetallesEnLaMismaTransaccion() throws Exception {
        var d=detalles.agregar(historia.id,atencion("GENERAL","Primera atención del paciente")); em.flush();
        mvc.perform(delete("/api/historias/"+historia.id).with(auth())).andExpect(status().isNoContent());
        String hid=historia.id,did=d.id; em.flush(); em.clear();
        assertTrue(em.find(HistoriaClinica.class,hid).eliminado);
        assertTrue(em.find(DetalleHistoriaClinica.class,did).eliminado);
        mvc.perform(get("/api/historias/"+hid).with(auth())).andExpect(status().isNotFound());
    }
    @Test void noPermiteCruzarAtencionesEntreHistorias() throws Exception {
        var d=detalles.agregar(historia.id,atencion("GENERAL","Atención de la primera historia"));
        var otra=historias.crear(new Historia(paciente.id,usuario.id));
        mvc.perform(delete("/api/historias/"+otra.id+"/detalles/"+d.id).with(auth())).andExpect(status().isNotFound());
        assertFalse(d.eliminado);
    }
    @Test void protegeReferenciasActivasYPermiteBajaTrasCerrarHistoria() throws Exception {
        detalles.agregar(historia.id,atencion("GENERAL","Atención de referencia activa"));
        mvc.perform(delete("/api/pacientes/"+paciente.id).with(auth())).andExpect(status().isConflict());
        mvc.perform(delete("/api/medicos/"+medico.id).with(auth())).andExpect(status().isConflict());
        mvc.perform(delete("/api/usuarios/"+usuario.id).with(auth())).andExpect(status().isConflict());
        mvc.perform(delete("/api/historias/"+historia.id).with(auth())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/pacientes/"+paciente.id).with(auth())).andExpect(status().isNoContent());
    }
    @Test void fotosValidaContenidoReemplazaYEliminaSinExponerBytes() throws Exception {
        String url="/api/pacientes/"+paciente.id+"/foto";
        mvc.perform(multipart(url).file(new MockMultipartFile("archivo","falsa.png","image/png","no es imagen".getBytes())).with(auth()))
            .andExpect(status().isBadRequest());
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);
        var archivo=new MockMultipartFile("archivo","foto.png","image/png",out.toByteArray());
        mvc.perform(multipart(url).file(archivo).with(auth())).andExpect(status().isCreated()).andExpect(jsonPath("$.contenido").doesNotExist());
        String primera=paciente.foto.id;
        mvc.perform(get(url).with(auth())).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(out.toByteArray()));
        mvc.perform(multipart(url).file(archivo).with(auth())).andExpect(status().isCreated());
        assertTrue(em.find(FotoPaciente.class,primera).eliminado);
        mvc.perform(delete(url).with(auth())).andExpect(status().isNoContent());
        mvc.perform(get(url).with(auth())).andExpect(status().isNotFound());
    }
    @Test void modificaUsuarioSinCambiarClaveYEditaAtencion() throws Exception {
        String hash=usuario.clave;
        mvc.perform(put("/api/usuarios/"+usuario.id).with(auth()).contentType("application/json").content(json.writeValueAsString(new Cuenta("prueba",null))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.clave").doesNotExist());
        assertEquals(hash,usuario.clave);
        var d=detalles.agregar(historia.id,atencion("GENERAL","Atención inicial para modificar"));
        mvc.perform(put("/api/historias/"+historia.id+"/detalles/"+d.id).with(auth()).contentType("application/json").content(json.writeValueAsString(atencion("CONTROL","Control con evolución favorable y alta"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.detalleHistoria").value("Control con evolución favorable y alta"));
    }
}
