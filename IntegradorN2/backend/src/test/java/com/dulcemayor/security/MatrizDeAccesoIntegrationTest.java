package com.dulcemayor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * E6-02 · checklist "cada rol contra cada endpoint": para un endpoint representativo de cada módulo,
 * los roles permitidos no reciben 401/403 (pueden recibir 400 o 404 por los datos), el resto de los
 * roles recibe 403 y sin token se recibe 401. Los POST van con cuerpo vacío para no modificar datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatrizDeAccesoIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;

    private final Map<RolUsuario, String> tokens = new EnumMap<>(RolUsuario.class);

    @BeforeAll
    void crearUsuarios() throws Exception {
        for (RolUsuario rol : RolUsuario.values()) {
            String correo = "matriz-" + rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@dulcemayor.com.ar";
            usuarioService.crearUsuario(correo, rol, CLAVE, false);
            String cuerpo = mvc.perform(request(HttpMethod.POST, "/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombreUsuario\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\"}"))
                    .andReturn().getResponse().getContentAsString();
            tokens.put(rol, objectMapper.readTree(cuerpo).get("token").asText());
        }
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource({
            "GET,    /api/categorias,                JEFE ADMINISTRATIVO",
            "GET,    /api/productos,                 JEFE ADMINISTRATIVO",
            "GET,    /api/stock,                     JEFE ADMINISTRATIVO",
            "GET,    /api/promociones,               JEFE ADMINISTRATIVO",
            "GET,    /api/pedidos,                   JEFE ADMINISTRATIVO",
            "GET,    /api/facturas,                  JEFE ADMINISTRATIVO",
            "GET,    /api/pagos/factura/x,           JEFE ADMINISTRATIVO",
            "GET,    /api/reportes/filtros,          JEFE ADMINISTRATIVO",
            "GET,    /api/sucursales,                JEFE ADMINISTRATIVO",
            "GET,    /api/formas-de-pago,            JEFE ADMINISTRATIVO",
            "GET,    /api/usuarios/internos,         JEFE",
            "GET,    /api/estadisticas?anio=2026,    JEFE",
            "GET,    /api/newsletter/enviar,         JEFE",
            "GET,    /api/preventa/pedidos,          JEFE PREVENTOR",
            "GET,    /api/distribucion/ruta,         DISTRIBUIDOR",
            "GET,    /api/puntos-de-venta,           JEFE ADMINISTRATIVO PREVENTOR",
            "POST,   /api/puntos-de-venta,           JEFE PREVENTOR",
            "GET,    /api/departamentos,             JEFE ADMINISTRATIVO PREVENTOR DISTRIBUIDOR MINORISTA",
            "POST,   /api/departamentos,             JEFE ADMINISTRATIVO",
            "DELETE, /api/localidades/x,             JEFE ADMINISTRATIVO",
            "GET,    /api/minorista/compras,         MINORISTA",
            "GET,    /api/minorista/perfil,          MINORISTA",
            "POST,   /api/imagenes,                  JEFE ADMINISTRATIVO",
            "GET,    /api/auth/yo,                   JEFE ADMINISTRATIVO PREVENTOR DISTRIBUIDOR MINORISTA"
    })
    void cadaRolSoloAccedeASusEndpoints(String metodo, String ruta, String rolesPermitidos) throws Exception {
        Set<RolUsuario> permitidos = Arrays.stream(rolesPermitidos.split(" "))
                .map(RolUsuario::valueOf).collect(Collectors.toSet());

        assertThat(estado(metodo, ruta, null)).as("sin token").isEqualTo(401);
        for (RolUsuario rol : RolUsuario.values()) {
            int estado = estado(metodo, ruta, tokens.get(rol));
            if (permitidos.contains(rol)) {
                assertThat(estado).as("%s debería poder usar %s %s", rol, metodo, ruta).isNotIn(401, 403);
            } else {
                assertThat(estado).as("%s no debería poder usar %s %s", rol, metodo, ruta).isEqualTo(403);
            }
        }
    }

    private int estado(String metodo, String ruta, String token) throws Exception {
        var pedido = request(HttpMethod.valueOf(metodo), ruta);
        if (!metodo.equals("GET") && !metodo.equals("DELETE")) {
            pedido.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (token != null) {
            pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(pedido).andReturn().getResponse().getStatus();
    }
}
