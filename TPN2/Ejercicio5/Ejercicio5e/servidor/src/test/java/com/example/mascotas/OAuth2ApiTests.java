package com.example.mascotas;

import com.example.mascotas.repositorios.UsuarioRepositorio;
import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"debug=false","spring.datasource.url=jdbc:h2:mem:oauth-api;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class OAuth2ApiTests {
    static final String ISSUER="http://127.0.0.1:9000";
    static final com.nimbusds.jose.jwk.RSAKey KEY;
    static final HttpServer JWKS;
    static {
        try {
            var gen=KeyPairGenerator.getInstance("RSA"); gen.initialize(2048); var pair=gen.generateKeyPair();
            KEY=new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate()).keyID("test-key").build();
            JWKS=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            JWKS.createContext("/jwks",ex -> {
                byte[] body=new JWKSet(KEY.toPublicJWK()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type","application/json"); ex.sendResponseHeaders(200,body.length);
                ex.getResponseBody().write(body); ex.close();
            }); JWKS.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.oauth.jwk-set-uri", () -> "http://127.0.0.1:"+JWKS.getAddress().getPort()+"/jwks");
    }
    @AfterAll static void cerrar() { JWKS.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepositorio usuarios;
    String token(String subject,String issuer,String audience,String scope,Instant expiration) {
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(subject).audience(List.of(audience))
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiration).claim("scope",scope).claim("name",subject).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY))).encode(
                JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),claims)).getTokenValue();
    }
    String token(String subject) { return token(subject,ISSUER,"ejercicio5-api","api.read api.write",Instant.now().plusSeconds(300)); }
    String subject() { return "cuenta-"+UUID.randomUUID(); }
    String perfil(String token) throws Exception {
        return json.readTree(mvc.perform(get("/api/usuarios/me").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.clave").doesNotExist())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }
    String mascota(String token,String nombre,String sexo) throws Exception {
        return json.readTree(mvc.perform(post("/api/mascotas").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nombre",nombre,"sexo",sexo,"tipo","PERRO"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }
    ResultActions votar(String token,String origen,String destino) throws Exception {
        return mvc.perform(post("/api/votos").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idMascotaPropia",origen,"idMascotaVotada",destino))));
    }
    @Test void apiSinBearerNoCreaSesionYNoAceptaJwtFalso() throws Exception {
        var res=mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized()).andReturn();
        assertThat(res.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/zonas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
    }
    @Test void validaFirmaEmisorAudienciaYExpiracionReales() throws Exception {
        String s=subject(), valido=token(s);
        perfil(valido);
        String[] invalidos={token(s,"https://otro","ejercicio5-api","api.read",Instant.now().plusSeconds(300)),
                token(s,ISSUER,"otro-cliente","api.read",Instant.now().plusSeconds(300)),
                token(s,ISSUER,"ejercicio5-api","api.read",Instant.now().minusSeconds(120)),
                valido.substring(0,valido.lastIndexOf('.')+1)+"AAAA"};
        for(String t:invalidos) mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());
    }
    @Test void scopeLecturaNoPermiteEscribir() throws Exception {
        String t=token(subject(),ISSUER,"ejercicio5-api","api.read",Instant.now().plusSeconds(300));
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+t)).andExpect(status().isOk());
        mvc.perform(post("/api/mascotas").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Luna\",\"sexo\":\"HEMBRA\",\"tipo\":\"PERRO\"}")).andExpect(status().isForbidden());
        String sinScope=token(subject(),ISSUER,"ejercicio5-api","openid profile",Instant.now().plusSeconds(300));
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+sinScope)).andExpect(status().isForbidden());
    }
    @Test void identidadPersisteEntreTokensYDistingueSujetosConMismoNombre() throws Exception {
        String subject=subject(), id=perfil(token(subject));
        assertThat(perfil(token(subject))).isEqualTo(id);
        assertThat(perfil(token(subject()))).isNotEqualTo(id);
        assertThat(usuarios.findById(id).orElseThrow().getOauthSubject()).isEqualTo(subject);
    }
    @Test void conservaCrudYAislamientoDeMascotas() throws Exception {
        String a=token(subject()), b=token(subject()); String id=mascota(a,"Luna","HEMBRA"); perfil(b);
        mvc.perform(put("/api/mascotas/"+id).header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Robada\",\"sexo\":\"HEMBRA\",\"tipo\":\"PERRO\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/mascotas/"+id).header("Authorization","Bearer "+b)).andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas/explorar").param("idMascotaPropia",id).header("Authorization","Bearer "+b)).andExpect(status().isForbidden());
        mvc.perform(put("/api/mascotas/"+id).header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lunita\",\"sexo\":\"HEMBRA\",\"tipo\":\"PERRO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Lunita"));
        mvc.perform(delete("/api/mascotas/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
    }
    @Test void conservaExploracionVotosYMatchConIdentidadesOAuth() throws Exception {
        String a=token(subject()), b=token(subject()), tercero=token(subject());
        String origen=mascota(a,"Luna","HEMBRA"), destino=mascota(b,"Toby","MACHO");
        mvc.perform(get("/api/mascotas/explorar").param("idMascotaPropia",origen).header("Authorization","Bearer "+a))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '"+destino+"')]").isNotEmpty());
        String voto=json.readTree(votar(a,origen,destino).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        votar(a,origen,destino).andExpect(status().isConflict());
        mvc.perform(post("/api/votos/"+voto+"/responder").header("Authorization","Bearer "+tercero)).andExpect(status().isForbidden());
        mvc.perform(post("/api/votos/"+voto+"/responder").header("Authorization","Bearer "+b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.match").value(true));
    }
    @Test void usuarioInactivoNoPuedeUsarTokensVigentes() throws Exception {
        String t=token(subject()), id=perfil(t); var u=usuarios.findById(id).orElseThrow(); u.setBaja(new Date()); usuarios.saveAndFlush(u);
        mvc.perform(get("/api/usuarios/me").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());
    }
    @Test void fotoEsPrivadaYUploadExigePropietarioYContenidoValido() throws Exception {
        String a=token(subject()), b=token(subject()), id=mascota(a,"Luna","HEMBRA");
        mvc.perform(get("/api/mascotas/"+id+"/foto")).andExpect(status().isUnauthorized());
        var foto=new MockMultipartFile("archivo","f.png","image/png","no es imagen".getBytes());
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/"+id+"/foto").file(foto).header("Authorization","Bearer "+b)).andExpect(status().isForbidden());
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/"+id+"/foto").file(foto).header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
    }
}
