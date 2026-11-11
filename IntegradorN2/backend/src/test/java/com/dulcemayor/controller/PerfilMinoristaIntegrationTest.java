package com.dulcemayor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.entity.Localidad;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilMinoristaIntegrationTest {

    private static final String CLAVE = "Prueba1234!";
    private static final String PERFIL = "/api/minorista/perfil";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MinoristaRepository minoristas;
    @Autowired private LocalidadRepository localidades;

    @Test
    void elMinoristaEditaSusDatosYLaDireccionQuedaEnLaLocalidadCorrecta() throws Exception {
        String token = tokenDeMinorista();
        Localidad elegida = localidades.findByEliminadoFalse(Sort.by("nombre")).getLast();

        mvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Marta"))
                .andExpect(jsonPath("$.localidadId").isEmpty());

        mvc.perform(put(PERFIL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("Martina", elegida.getId(), "-32.92")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Martina"));

        mvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Martina"))
                .andExpect(jsonPath("$.localidadId").value(elegida.getId()))
                .andExpect(jsonPath("$.localidad").value(elegida.getNombre()))
                .andExpect(jsonPath("$.departamentoId").value(elegida.getDepartamento().getId()))
                .andExpect(jsonPath("$.calle").value("Belgrano"))
                .andExpect(jsonPath("$.latitud").value(-32.92));
    }

    @Test
    void validaLosDatosDelPerfil() throws Exception {
        String token = tokenDeMinorista();
        String localidadId = localidades.findByEliminadoFalse(Sort.by("nombre")).getFirst().getId();

        mvc.perform(put(PERFIL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("", localidadId, "-32.92")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nombre"));
        mvc.perform(put(PERFIL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("Marta", localidadId, "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("latitud"));
        mvc.perform(put(PERFIL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("Marta", "inexistente", "-32.92")))
                .andExpect(status().isNotFound());
    }

    @Test
    void elMinoristaUsaElComponenteDeDireccionPeroNoEntraAlPanelInterno() throws Exception {
        String token = tokenDeMinorista();
        String departamentoId = localidades.findByEliminadoFalse(Sort.by("nombre")).getFirst()
                .getDepartamento().getId();

        mvc.perform(get("/api/departamentos").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/departamentos/" + departamentoId + "/localidades")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/departamentos").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unPreventorNoTienePerfilDeMinorista() throws Exception {
        String correo = "preventor-" + UUID.randomUUID() + "@prueba.com";
        usuarioService.crearUsuario(correo, RolUsuario.PREVENTOR, CLAVE, false);

        mvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, login(correo)))
                .andExpect(status().isForbidden());
    }

    private static String perfil(String nombre, String localidadId, String latitud) {
        return """
                {"nombre":"%s","apellido":"Prueba","fechaNacimiento":"1985-01-01","localidadId":"%s",
                 "calle":"Belgrano","numeracion":"1200","latitud":%s,"longitud":-68.84}
                """.formatted(nombre, localidadId, latitud);
    }

    private String tokenDeMinorista() throws Exception {
        String correo = "minorista-" + UUID.randomUUID() + "@prueba.com";
        Usuario usuario = usuarioService.crearUsuario(correo, RolUsuario.MINORISTA, CLAVE, false);
        Minorista minorista = new Minorista();
        minorista.setNombre("Marta");
        minorista.setApellido("Prueba");
        minorista.setFechaNacimiento(LocalDate.of(1985, 1, 1));
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento(String.valueOf(10_000_000 + (long) (Math.random() * 80_000_000)));
        minorista.setUsuario(usuario);
        minoristas.save(minorista);
        return login(correo);
    }

    private String login(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(cuerpo).get("token").asText();
    }
}
