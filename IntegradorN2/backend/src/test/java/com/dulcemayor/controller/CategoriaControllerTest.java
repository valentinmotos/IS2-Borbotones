package com.dulcemayor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.config.SecurityConfig;
import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErroresSeguridadHandler;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.JwtService;
import com.dulcemayor.service.CategoriaService;

/** E0-08: endpoints heredados de AbstractCrudController y sus códigos HTTP. */
@WebMvcTest(controllers = CategoriaController.class)
@Import({ SecurityConfig.class, ErroresSeguridadHandler.class, JwtService.class })
@ActiveProfiles("test")
@WithMockUser(roles = "ADMINISTRATIVO")
class CategoriaControllerTest {

    private static final String BASE = "/api/categorias";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService service;

    /** Lo usa JwtFiltro; en estos tests la sesión la simula @WithMockUser. */
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void listarDevuelveLasActivas() throws Exception {
        when(service.listarActivos()).thenReturn(List.of(new CategoriaResponse("id-1", "Alfajores")));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-1"))
                .andExpect(jsonPath("$[0].nombre").value("Alfajores"))
                .andExpect(jsonPath("$[0].eliminado").doesNotExist());
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(service.crear(new CategoriaRequest("Alfajores")))
                .thenReturn(new CategoriaResponse("id-1", "Alfajores"));

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Alfajores\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void crearDuplicadoDevuelve409ConMensajeYCampo() throws Exception {
        when(service.crear(any())).thenThrow(new DuplicadoException("Ya existe una categoría con ese nombre.", "nombre"));

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"alfajores\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una categoría con ese nombre."))
                .andExpect(jsonPath("$.campo").value("nombre"));
    }

    @Test
    void crearSinNombreDevuelve400SinLlamarAlService() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre de la categoría es obligatorio."))
                .andExpect(jsonPath("$.campo").value("nombre"));
        verify(service, never()).crear(any());
    }

    @Test
    void modificarInexistenteDevuelve404() throws Exception {
        when(service.modificar(any(), any())).thenThrow(new RecursoNoEncontradoException("No se encontró la categoría."));

        mvc.perform(put(BASE + "/nada").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró la categoría."));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete(BASE + "/id-1")).andExpect(status().isNoContent());
        verify(service).eliminar("id-1");
    }
}
