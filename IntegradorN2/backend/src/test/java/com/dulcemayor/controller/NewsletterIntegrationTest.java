package com.dulcemayor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:newsletter;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.newsletter.cron=0 0 9 5 * *", "APP_URL_PUBLIC=https://tienda.example/"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NewsletterIntegrationTest {
    private static final String RUTA = "/api/newsletter/enviar";

    @Autowired private MockMvc mvc;
    @Autowired private ProductoRepository productos;
    @Autowired private VigenciaPrecioRepository precios;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MinoristaRepository minoristas;
    @Autowired private ScheduledTaskHolder tareas;
    @MockitoBean private JavaMailSender correo;

    @Test
    void enviaHtmlSoloAClientesActivosConOfertasDisponiblesYPrecioVigente() throws Exception {
        for (String codigo : new String[] {"ALF-001", "CAR-001", "CHI-001", "GOM-001"}) {
            productos.findByCodigoIgnoreCase(codigo).orElseThrow().setEnOferta(true);
        }
        var chicle = productos.findByCodigoIgnoreCase("CHI-001").orElseThrow();
        precios.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(chicle.getId()).getFirst()
                .setFechaDesde(LocalDate.now().plusDays(1));
        productos.findByCodigoIgnoreCase("GOM-001").orElseThrow().setEliminado(true);
        minorista("pendiente@example.com", "85000001", false, false);
        minorista("baja@example.com", "85000002", true, true);
        when(correo.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));

        mvc.perform(post(RUTA).with(user("jefe").roles("JEFE")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ofertas").value(1))
                .andExpect(jsonPath("$.destinatariosProgramados").value(1));

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo, timeout(5000)).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();
        assertThat(mensaje.getSubject()).isEqualTo("Ofertas del mes · Dulce Mayor");
        assertThat(mensaje.getAllRecipients()).extracting(Object::toString)
                .containsExactly("minorista@dulcemayor.com.ar");
        assertThat(mensaje.getContentType()).contains("text/html");
        assertThat(mensaje.getContent().toString())
                .contains("Alfajor de chocolate", "6.000,00", "https://tienda.example/catalogo")
                .doesNotContain("Chupetín frutal", "Chicle de menta", "Chocolate con leche", "Gomitas ácidas");
    }

    @Test
    void restringeElEnvioManualYOmiteElCorreoSiNoHayOfertas() throws Exception {
        mvc.perform(post(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA).with(user("admin").roles("ADMINISTRATIVO"))).andExpect(status().isForbidden());
        mvc.perform(post(RUTA).with(user("jefe").roles("JEFE")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ofertas").value(0))
                .andExpect(jsonPath("$.destinatariosProgramados").value(0));
        verifyNoInteractions(correo);
    }

    @Test
    void programaElEnvioElDiaCincoALasNueveDeMendoza() {
        var tarea = tareas.getScheduledTasks().stream()
                .map(agendada -> agendada.getTask())
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(cron -> cron.getRunnable().toString().endsWith("NewsletterScheduler.enviarOfertasDelMes"))
                .findFirst().orElseThrow();
        var reloj = Clock.fixed(Instant.parse("2026-11-05T12:00:01Z"), ZoneOffset.UTC);
        assertThat(tarea.getTrigger().nextExecution(new SimpleTriggerContext(reloj)))
                .isEqualTo(Instant.parse("2026-12-05T12:00:00Z"));
    }

    private void minorista(String email, String documento, boolean activo, boolean baja) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(email);
        usuario.setRol(RolUsuario.MINORISTA);
        usuario.setClave("clave de prueba");
        usuario.setActivo(activo);
        usuario.setEliminado(baja);
        Minorista minorista = new Minorista();
        minorista.setNombre("Cliente");
        minorista.setApellido("Newsletter");
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento(documento);
        minorista.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        minorista.setUsuario(usuarios.save(usuario));
        minoristas.save(minorista);
    }
}
