package com.borbotones.videojuegos;

import com.borbotones.videojuegos.dto.RegistroForm;
import com.borbotones.videojuegos.entities.Usuario;
import com.borbotones.videojuegos.repositories.UsuarioRepository;
import com.borbotones.videojuegos.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void catalogoEsPublicoYEnviaCabeceraCsp() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void loginYRegistroSonPublicos() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/registro")).andExpect(status().isOk());
    }

    @Test
    void altaDeVideojuegoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/altaVideojuego"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void usuarioAutenticadoPuedeAbrirAltaDeVideojuego() throws Exception {
        mockMvc.perform(get("/altaVideojuego"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void operacionSinTokenCsrfEsRechazada() throws Exception {
        mockMvc.perform(post("/bajaVideojuego").param("id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("nombre", "Maximo")
                        .param("email", "maximo@example.com")
                        .param("password", "Segura123")
                        .param("confirmarPassword", "Segura123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void registroGuardaLaContrasenaConBcrypt() {
        RegistroForm form = new RegistroForm();
        form.setNombre("Maximo");
        form.setEmail("bcrypt@example.com");
        form.setPassword("Segura123");
        form.setConfirmarPassword("Segura123");

        Usuario guardado = usuarioService.registrar(form);
        Usuario recuperado = usuarioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getPassword()).isNotEqualTo("Segura123");
        assertThat(passwordEncoder.matches("Segura123", recuperado.getPassword())).isTrue();
    }

    @Test
    void registroValidoConCsrfRedirigeAlLogin() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Usuario Seguro")
                        .param("email", "registro@example.com")
                        .param("password", "Segura123")
                        .param("confirmarPassword", "Segura123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registro"));
    }
}
