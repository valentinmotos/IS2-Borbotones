package com.borbotones.autorizacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"debug=false","spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ProtocoloTests {
    @Autowired MockMvc mvc;
    @Test void publicaDiscoveryYClavesSinExponerClavePrivada() throws Exception {
        mvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.issuer").value("http://127.0.0.1:9000"))
                .andExpect(jsonPath("$.authorization_endpoint").exists());
        mvc.perform(get("/oauth2/jwks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }
    @Test void elLoginLocalRequiereCsrf() throws Exception {
        mvc.perform(post("/login").param("username","usuario").param("password","Usuario123!"))
                .andExpect(status().isForbidden());
    }
    @Test void noAceptaPasswordGrantNiClientCredentials() throws Exception {
        for (String grant : new String[]{"password", "client_credentials"}) {
            mvc.perform(post("/oauth2/token").with(httpBasic("ejercicio5-cliente","cliente-local-demo-2026"))
                    .contentType("application/x-www-form-urlencoded").param("grant_type",grant))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void autorizacionExigePkceYRedirectRegistrado() throws Exception {
        mvc.perform(get("/oauth2/authorize").with(user("usuario").roles("USUARIO"))
                .param("response_type","code").param("client_id","ejercicio5-cliente")
                .param("redirect_uri","http://127.0.0.1:8080/login/oauth2/code/local")
                .param("scope","openid api.read").param("state","estado-seguro"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/oauth2/authorize").with(user("usuario").roles("USUARIO"))
                .param("response_type","code").param("client_id","ejercicio5-cliente")
                .param("redirect_uri","https://otro.example/callback")
                .param("scope","openid").param("state","estado-seguro"))
                .andExpect(status().isBadRequest());
    }
    @Test void registroGuardaBCryptYNoPermiteAsignarRolAdmin() throws Exception {
        String nombre="nuevo_"+java.util.UUID.randomUUID().toString().substring(0,8);
        mvc.perform(post("/registro").with(csrf()).param("username",nombre).param("password","Segura123!")
                .param("confirmacion","Segura123!").param("rol","ADMIN"))
                .andExpect(status().isOk()).andExpect(model().attribute("exito",true));
        var u=usuarios.loadUserByUsername(nombre);
        org.assertj.core.api.Assertions.assertThat(u.getPassword()).startsWith("{bcrypt}");
        org.assertj.core.api.Assertions.assertThat(u.getAuthorities()).extracting("authority").containsExactly("ROLE_USUARIO");
        mvc.perform(post("/registro").with(csrf()).param("username",nombre).param("password","Segura123!")
                .param("confirmacion","Segura123!"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }
    @Autowired org.springframework.security.provisioning.JdbcUserDetailsManager usuarios;
}
