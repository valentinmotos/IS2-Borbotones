package com.dulcemayor.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** E1-03: integración del ABM y consulta de localidades por departamento. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UbicacionIntegrationTest {

    private static final String DEPARTAMENTOS = "/api/departamentos";
    private static final String LOCALIDADES = "/api/localidades";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void abmDeLocalidadSeAsociaAlDepartamentoYRechazaDuplicados() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String nombreDepartamento = "E1-03 " + sufijo;
        String nombreLocalidad = "Localidad " + sufijo;

        String respuestaDepartamento = mvc.perform(post(DEPARTAMENTOS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombreDepartamento + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode departamento = objectMapper.readTree(respuestaDepartamento);
        String departamentoId = departamento.get("id").asText();

        mvc.perform(post(LOCALIDADES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(localidadJson(nombreLocalidad, "9817", departamentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departamentoId").value(departamentoId))
                .andExpect(jsonPath("$.departamentoNombre").value(nombreDepartamento));

        mvc.perform(post(DEPARTAMENTOS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"e1-03 " + sufijo + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("nombre"));

        mvc.perform(post(LOCALIDADES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(localidadJson(nombreLocalidad.toUpperCase(), "9818", departamentoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("nombre"));

        mvc.perform(post(LOCALIDADES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(localidadJson("Otra " + sufijo, "9817", departamentoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("codigoPostal"));

        mvc.perform(get(DEPARTAMENTOS + "/" + departamentoId + "/localidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem(nombreLocalidad)))
                .andExpect(jsonPath("$[*].eliminado").doesNotExist());

        mvc.perform(get(LOCALIDADES + "/codigo-postal/9817"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombreLocalidad));

        mvc.perform(delete(DEPARTAMENTOS + "/" + departamentoId))
                .andExpect(status().isBadRequest());

        String respuestaLocalidades = mvc.perform(get(DEPARTAMENTOS + "/" + departamentoId + "/localidades"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String localidadId = objectMapper.readTree(respuestaLocalidades).get(0).get("id").asText();
        mvc.perform(delete(LOCALIDADES + "/" + localidadId)).andExpect(status().isNoContent());
        mvc.perform(get(DEPARTAMENTOS + "/" + departamentoId + "/localidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", not(hasItem(nombreLocalidad))));
        mvc.perform(delete(DEPARTAMENTOS + "/" + departamentoId)).andExpect(status().isNoContent());
    }

    private static String localidadJson(String nombre, String codigoPostal, String departamentoId) {
        return "{\"nombre\":\"" + nombre + "\",\"codigoPostal\":\"" + codigoPostal
                + "\",\"departamentoId\":\"" + departamentoId + "\"}";
    }
}
