package com.example.mascotas;

import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.repositorios.UsuarioRepositorio;
import com.fasterxml.jackson.databind.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import java.time.Instant;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "logging.level.root=WARN",
        "spring.datasource.url=jdbc:h2:mem:mascotas-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg="})
@AutoConfigureMockMvc
class SeguridadIntegracionTests {
    private static final String SECRET="YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg=";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired PasswordEncoder encoder;

    record Cuenta(String id, String mail, String token) {}

    private String zona() throws Exception {
        return json.readTree(mvc.perform(get("/api/zonas")).andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
    }
    private Cuenta cuenta() throws Exception {
        String mail=UUID.randomUUID()+"@test.com";
        var result=mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("nombre","Ana","apellido","Perez","mail",mail,"clave","segura123","clave2","segura123","idZona",zona()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuario.clave").doesNotExist())
                .andExpect(jsonPath("$.tokenType").value("Bearer")).andReturn();
        JsonNode data=json.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();
        return new Cuenta(data.at("/usuario/id").asText(),mail,data.get("accessToken").asText());
    }
    private String mascota(Cuenta c, String nombre, String sexo) throws Exception {
        return json.readTree(mvc.perform(post("/api/mascotas").header("Authorization","Bearer "+c.token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nombre",nombre,"sexo",sexo,"tipo","PERRO"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }
    private ResultActions votar(Cuenta c, String origen, String destino) throws Exception {
        return mvc.perform(post("/api/votos").header("Authorization","Bearer "+c.token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idMascotaPropia",origen,"idMascotaVotada",destino))));
    }
    private String firmado(Cuenta c, Instant expiracion, String emisor, int version) {
        return Jwts.builder().issuer(emisor).subject(c.id).claim("tipo","access").claim("version",version)
                .issuedAt(Date.from(Instant.now().minusSeconds(60))).expiration(Date.from(expiracion))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)),Jwts.SIG.HS256).compact();
    }

    @Test void rutasPrivadasRequierenBearerYNoAceptanBasicNiQueryToken() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/votos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas/algo/foto")).andExpect(status().isUnauthorized());
        Cuenta c=cuenta();
        mvc.perform(get("/api/mascotas").param("token",c.token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas").header("Authorization","Basic YTpi")).andExpect(status().isUnauthorized());
    }
    @Test void registroUsaBcryptYLoginNoExponeCredenciales() throws Exception {
        Cuenta c=cuenta(); Usuario u=usuarios.findById(c.id).orElseThrow();
        assertThat(u.getClave()).isNotEqualTo("segura123");
        assertThat(encoder.matches("segura123",u.getClave())).isTrue();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("mail",c.mail.toUpperCase(Locale.ROOT),"clave","segura123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.clave").doesNotExist());
        mvc.perform(get("/api/usuarios/me").header("Authorization","Bearer "+c.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(c.id))
                .andExpect(jsonPath("$.clave").doesNotExist()).andExpect(jsonPath("$.tokenVersion").doesNotExist());
    }
    @Test void credencialesIncorrectasYUsuarioInactivoSonRechazados() throws Exception {
        Cuenta c=cuenta();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("mail",c.mail,"clave","incorrecta"))))
                .andExpect(status().isUnauthorized());
        Usuario u=usuarios.findById(c.id).orElseThrow(); u.setBaja(new Date()); usuarios.saveAndFlush(u);
        mvc.perform(get("/api/usuarios/me").header("Authorization","Bearer "+c.token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("mail",c.mail,"clave","segura123")))).andExpect(status().isUnauthorized());
    }
    @Test void jwtVencidoAdulteradoEmisorIncorrectoOSinFirmaSonRechazados() throws Exception {
        Cuenta c=cuenta();
        String[] invalidos={"no-es-jwt",c.token.substring(0,c.token.lastIndexOf('.')+1)+"AAAA",
                firmado(c,Instant.now().minusSeconds(1),"tinder-mascotas",0),
                firmado(c,Instant.now().plusSeconds(60),"otro-emisor",0),
                firmado(c,Instant.now().plusSeconds(60),"tinder-mascotas",10),
                Jwts.builder().subject(c.id).compact()};
        for (String token:invalidos) mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void logoutInvalidaTokensEmitidosInclusoSiSeReutilizan() throws Exception {
        Cuenta c=cuenta();
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+c.token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+c.token)).andExpect(status().isUnauthorized());
        String nuevo=json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("mail",c.mail,"clave","segura123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+nuevo)).andExpect(status().isOk());
    }
    @Test void validaRegistroDuplicadosYDatosDeMascota() throws Exception {
        Cuenta c=cuenta();
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("nombre","Ana","apellido","Perez","mail",c.mail.toUpperCase(Locale.ROOT),"clave","segura123","clave2","segura123","idZona",zona()))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("nombre","Ana","apellido","Perez","mail","a@b.com","clave","segura123","clave2","diferente","idZona",zona()))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/mascotas").header("Authorization","Bearer "+c.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"sexo\":null,\"tipo\":\"PERRO\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/mascotas").header("Authorization","Bearer "+c.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Luna\",\"sexo\":\"HEMBRA\",\"tipo\":\"OTRO\"}")).andExpect(status().isBadRequest());
    }
    @Test void noPermiteEditarEliminarExplorarOVotarConMascotasAjenas() throws Exception {
        Cuenta a=cuenta(), b=cuenta(); String id=mascota(a,"Luna","HEMBRA"), otra=mascota(b,"Toby","MACHO");
        mvc.perform(put("/api/mascotas/"+id).header("Authorization","Bearer "+b.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Robada\",\"sexo\":\"HEMBRA\",\"tipo\":\"PERRO\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/mascotas/"+id).header("Authorization","Bearer "+b.token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas/explorar").param("idMascotaPropia",id).header("Authorization","Bearer "+b.token)).andExpect(status().isForbidden());
        votar(b,id,otra).andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+b.token)).andExpect(jsonPath("$[?(@.id == '"+id+"')]").isEmpty());
    }
    @Test void crudMascotasYBajaLogica() throws Exception {
        Cuenta c=cuenta(); String id=mascota(c,"Luna","HEMBRA");
        mvc.perform(put("/api/mascotas/"+id).header("Authorization","Bearer "+c.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lunita\",\"sexo\":\"HEMBRA\",\"tipo\":\"GATO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Lunita"));
        mvc.perform(delete("/api/mascotas/"+id).header("Authorization","Bearer "+c.token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mascotas").header("Authorization","Bearer "+c.token)).andExpect(content().json("[]"));
        mvc.perform(delete("/api/mascotas/"+id).header("Authorization","Bearer "+c.token)).andExpect(status().isNotFound());
    }
    @Test void votosRecibidosSoloPuedeAceptarlosElDestinatario() throws Exception {
        Cuenta a=cuenta(), b=cuenta(), tercero=cuenta();
        String origen=mascota(a,"Luna","HEMBRA"), destino=mascota(b,"Toby","MACHO");
        String id=json.readTree(votar(a,origen,destino).andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").value(false)).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/votos").header("Authorization","Bearer "+tercero.token)).andExpect(content().json("[]"));
        mvc.perform(post("/api/votos/"+id+"/responder").header("Authorization","Bearer "+a.token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/votos/"+id+"/responder").header("Authorization","Bearer "+b.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.match").value(true));
        mvc.perform(post("/api/votos/"+id+"/responder").header("Authorization","Bearer "+b.token)).andExpect(status().isConflict());
    }
    @Test void votoMutuoGeneraMatchYNoPermiteDuplicadosNiAutovotos() throws Exception {
        Cuenta a=cuenta(), b=cuenta(); String origen=mascota(a,"Luna","HEMBRA"), destino=mascota(b,"Toby","MACHO");
        votar(a,origen,origen).andExpect(status().isBadRequest());
        votar(a,origen,destino).andExpect(status().isCreated());
        votar(a,origen,destino).andExpect(status().isConflict());
        votar(b,destino,origen).andExpect(status().isCreated()).andExpect(jsonPath("$.match").value(true));
        mvc.perform(get("/api/votos").header("Authorization","Bearer "+a.token)).andExpect(jsonPath("$[0].match").value(true));
    }
    @Test void exploracionFiltraCompatibilidadBajasYVotos() throws Exception {
        Cuenta a=cuenta(), b=cuenta(); String origen=mascota(a,"Luna","HEMBRA"), destino=mascota(b,"Toby","MACHO");
        String incompatible=mascota(b,"Otra","HEMBRA");
        mvc.perform(get("/api/mascotas/explorar").param("idMascotaPropia",origen).header("Authorization","Bearer "+a.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '"+destino+"')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == '"+incompatible+"')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == '"+origen+"')]").isEmpty());
        votar(a,origen,incompatible).andExpect(status().isBadRequest());
        votar(a,origen,destino).andExpect(status().isCreated());
        mvc.perform(get("/api/mascotas/explorar").param("idMascotaPropia",origen).header("Authorization","Bearer "+a.token))
                .andExpect(jsonPath("$[?(@.id == '"+destino+"')]").isEmpty());
        mvc.perform(delete("/api/mascotas/"+destino).header("Authorization","Bearer "+b.token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/votos").header("Authorization","Bearer "+a.token)).andExpect(content().json("[]"));
    }
    @Test void fotosVerificaContenidoAutorizacionYPreservaFotoAlEditar() throws Exception {
        Cuenta a=cuenta(), b=cuenta(); String id=mascota(a,"Luna","HEMBRA");
        var salida=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",salida);
        var foto=new MockMultipartFile("archivo","foto.png","image/png",salida.toByteArray());
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/"+id+"/foto").file(foto).header("Authorization","Bearer "+b.token))
                .andExpect(status().isForbidden());
        var falsa=new MockMultipartFile("archivo","foto.png","image/png","<script>mal</script>".getBytes());
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/"+id+"/foto").file(falsa).header("Authorization","Bearer "+a.token))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/"+id+"/foto").file(foto).header("Authorization","Bearer "+a.token))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/mascotas/"+id).header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lunita\",\"sexo\":\"HEMBRA\",\"tipo\":\"PERRO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tieneFoto").value(true));
        mvc.perform(get("/api/mascotas/"+id+"/foto").header("Authorization","Bearer "+b.token))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
    @Test void perfilSeResuelveDesdeJwtSinPoderSuplantarId() throws Exception {
        Cuenta a=cuenta(), b=cuenta();
        mvc.perform(put("/api/usuarios/me").header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id",b.id,"nombre","Nuevo","apellido","Perez","idZona",zona()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(a.id));
        assertThat(usuarios.findById(b.id).orElseThrow().getNombre()).isEqualTo("Ana");
    }
}
