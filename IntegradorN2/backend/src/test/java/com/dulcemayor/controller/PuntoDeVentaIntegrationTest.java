package com.dulcemayor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PuntoDeVentaIntegrationTest {

    private static final String CLAVE = "Prueba1234!";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PreventorRepository preventores;
    @Autowired private MinoristaRepository minoristas;
    @Autowired private LocalidadRepository localidades;

    @Test
    void unPreventorDaDeAltaUnKioscoYApareceEnElMapaDeSuZona() throws Exception {
        String token = tokenDePreventor();
        String minoristaId = crearMinorista();
        Localidad localidad = localidades.findByEliminadoFalse(Sort.by("nombre")).getFirst();
        String zona = localidad.getDepartamento().getId();
        String nombre = "Kiosco " + UUID.randomUUID();

        String cuerpo = mvc.perform(post("/api/puntos-de-venta").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(nombre, minoristaId, localidad.getId(), "-32.8878", "-68.8421")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreComercio").value(nombre))
                .andExpect(jsonPath("$.idDepartamento").value(zona))
                .andExpect(jsonPath("$.latitud").value(-32.8878))
                .andExpect(jsonPath("$.longitud").value(-68.8421))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(cuerpo).get("id").asText();

        mvc.perform(get("/api/puntos-de-venta").param("departamentoId", zona)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].nombreComercio").value(nombre));
        mvc.perform(get("/api/puntos-de-venta").param("departamentoId", "otra-zona")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/puntos-de-venta/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calle").value("San Martín"));
    }

    @Test
    void rechazaAltaSinUbicacionOConMinoristaInexistente() throws Exception {
        String token = tokenDePreventor();
        String minoristaId = crearMinorista();
        String localidadId = localidades.findByEliminadoFalse(Sort.by("nombre")).getFirst().getId();

        mvc.perform(post("/api/puntos-de-venta").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("Sin mapa", minoristaId, localidadId, "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Marcá la ubicación del comercio en el mapa."));
        mvc.perform(post("/api/puntos-de-venta").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("Sin minorista", "inexistente", localidadId, "-32.9", "-68.8")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unAdministrativoConsultaPeroNoDaDeAlta() throws Exception {
        String token = token(RolUsuario.ADMINISTRATIVO);

        mvc.perform(get("/api/puntos-de-venta").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/puntos-de-venta").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("Kiosco", "x", "y", "-32.9", "-68.8")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unDistribuidorNoVePuntosDeVenta() throws Exception {
        mvc.perform(get("/api/puntos-de-venta")
                        .header(HttpHeaders.AUTHORIZATION, token(RolUsuario.DISTRIBUIDOR)))
                .andExpect(status().isForbidden());
    }

    private static String alta(String nombre, String minoristaId, String localidadId, String latitud,
            String longitud) {
        return """
                {"nombreComercio":"%s","minoristaId":"%s","localidadId":"%s","calle":"San Martín",
                 "numeracion":"850","latitud":%s,"longitud":%s}
                """.formatted(nombre, minoristaId, localidadId, latitud, longitud);
    }

    private String tokenDePreventor() throws Exception {
        String correo = "preventor-" + UUID.randomUUID() + "@prueba.com";
        Usuario usuario = usuarioService.crearUsuario(correo, RolUsuario.PREVENTOR, CLAVE, false);
        Preventor preventor = new Preventor();
        preventor.setNombre("Pedro");
        preventor.setApellido("Prueba");
        preventor.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        preventor.setTipoDocumento(TipoDocumento.DNI);
        preventor.setNumeroDocumento(documento());
        preventor.setLegajo("P-" + UUID.randomUUID().toString().substring(0, 8));
        preventor.setUsuario(usuario);
        preventores.save(preventor);
        return login(correo);
    }

    private String token(RolUsuario rol) throws Exception {
        String correo = rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com";
        usuarioService.crearUsuario(correo, rol, CLAVE, false);
        return login(correo);
    }

    private String login(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(cuerpo).get("token").asText();
    }

    private String crearMinorista() {
        Minorista minorista = new Minorista();
        minorista.setNombre("Marta");
        minorista.setApellido("Prueba");
        minorista.setFechaNacimiento(LocalDate.of(1985, 1, 1));
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento(documento());
        return minoristas.save(minorista).getId();
    }

    private static String documento() {
        return String.valueOf(10_000_000 + (long) (Math.random() * 80_000_000));
    }
}
