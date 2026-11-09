package com.dulcemayor.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * E0-08 · criterio de aceptación de punta a punta (H2): alta, duplicado rechazado con 409,
 * baja lógica y nombre reutilizable después de la baja. Incluye el mapper real de MapStruct.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaIntegrationTest {

    private static final String BASE = "/api/categorias";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String crear(String nombre) throws Exception {
        String cuerpo = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(cuerpo);
        return json.get("id").asText();
    }

    @Test
    void abmCompletoConDuplicadoRechazado() throws Exception {
        String id = crear("Turrones");

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" TURRÓNES \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una categoría con ese nombre."))
                .andExpect(jsonPath("$.campo").value("nombre"));

        mvc.perform(get(BASE)).andExpect(jsonPath("$[*].nombre", hasItem("Turrones")));

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(get(BASE)).andExpect(jsonPath("$[*].nombre", not(hasItem("Turrones"))));

        // Dada de baja, el nombre se puede volver a usar
        crear("Turrones");
    }
}
