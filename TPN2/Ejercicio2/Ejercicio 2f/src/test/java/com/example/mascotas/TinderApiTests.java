package com.example.mascotas;

import com.example.mascotas.cliente.TinderRestClient;
import com.example.mascotas.dto.Dto.*;
import com.example.mascotas.entidades.Mascota;
import com.example.mascotas.enumeracion.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.*;
import static org.assertj.core.api.Assertions.*;

/** Pruebas contra un servidor HTTP real, consumido con RestTemplate (sin mocks). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TinderApiTests {
    @LocalServerPort int port;
    @Autowired RestTemplate rest;
    @Autowired TransactionTemplate transactions;
    @PersistenceContext EntityManager entityManager;
    TinderRestClient client;
    String url, ana, juan;
    final String clave = "mascotas123";
    ZonaDto zona;
    MascotaDto luna, toby;

    @BeforeEach
    void preparar() {
        url = "http://localhost:" + port;
        client = new TinderRestClient(rest, url);
        String suffix = UUID.randomUUID().toString();
        ana = "ana." + suffix + "@test.com"; juan = "juan." + suffix + "@test.com";
        zona = client.crearZona(new ZonaRequest("Centro " + suffix, "Zona de prueba"));
        client.registrar(new UsuarioRequest("Ana", "Perez", ana, clave, clave, zona.id()));
        client.registrar(new UsuarioRequest("Juan", "Lopez", juan, clave, clave, zona.id()));
        luna = client.crearMascota(ana, clave, new MascotaRequest("Luna", Sexo.HEMBRA, Tipo.PERRO));
        toby = client.crearMascota(juan, clave, new MascotaRequest("Toby", Sexo.MACHO, Tipo.PERRO));
    }
    HttpHeaders auth(String mail) {
        HttpHeaders headers = new HttpHeaders(); headers.setBasicAuth(mail, clave, StandardCharsets.UTF_8); return headers;
    }
    <T> ResponseEntity<T> request(String path, HttpMethod method, Object body, String mail, Class<T> type) {
        HttpHeaders headers = mail == null ? new HttpHeaders() : auth(mail);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url + "/api" + path, method, new HttpEntity<>(body, headers), type);
    }
    void status(int expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(HttpClientErrorException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(expected));
    }

    @Test
    void flujoCompletoConRestTemplateYMatch() {
        assertThat(toby.usuarioNombre()).isEqualTo("Juan");
        assertThat(toby.usuarioApellido()).isEqualTo("Lopez");
        assertThat(client.login(new LoginRequest(ana, clave)).mail()).isEqualTo(ana);
        assertThat(client.mascotas(ana, clave)).extracting(MascotaDto::id).containsExactly(luna.id());
        assertThat(client.candidatos(ana, clave, luna.id())).extracting(MascotaDto::id).containsExactly(toby.id());
        VotoDto voto = client.votar(ana, clave, new VotoRequest(luna.id(), toby.id()));
        assertThat(voto.match()).isFalse();
        assertThat(client.candidatos(ana, clave, luna.id())).isEmpty();
        assertThat(request("/votos/recibidos", HttpMethod.GET, null, juan, VotoDto[].class).getBody()).hasSize(1);
        assertThat(client.responder(juan, clave, voto.id()).match()).isTrue();
        assertThat(client.matches(ana, clave)).extracting(VotoDto::id).containsExactly(voto.id());
        assertThat(client.matches(juan, clave)).extracting(VotoDto::id).containsExactly(voto.id());
        Date respuesta = client.responder(juan, clave, voto.id()).respuesta();
        assertThat(client.responder(juan, clave, voto.id()).respuesta()).isEqualTo(respuesta);
    }

    @Test
    void rutasDeInterfazYRecursosOriginales() {
        for (String route : List.of("/", "/login", "/registro", "/inicio", "/usuario/editar-perfil",
                "/mascota/mis-mascotas", "/mascota/editar-perfil?id=ejemplo&accion=Actualizar",
                "/mascota/explorar-mascotas", "/mascota/mascotas-de-baja", "/votos/recibidos", "/matches")) {
            ResponseEntity<String> page = rest.getForEntity(url + route, String.class);
            assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(page.getBody()).contains("/css/one-page-wonder.min.css", "id=\"exposition\"", "/views.js");
        }
        for (String asset : List.of("/views.js", "/api.js", "/app.js", "/styles.css", "/css/one-page-wonder.min.css",
                "/vendor/bootstrap/css/bootstrap.min.css", "/vendor/jquery/jquery.min.js",
                "/vendor/bootstrap/js/bootstrap.bundle.min.js", "/img/m1.jpeg", "/img/m2.jpg", "/img/m3.jpeg")) {
            assertThat(rest.getForEntity(url + asset, byte[].class).getBody()).isNotEmpty();
        }
        status(404, () -> rest.getForEntity(url + "/api/no-existe", String.class));
    }

    @Test
    void credencialesPermisosYDatosPrivados() {
        status(401, () -> request("/mascotas", HttpMethod.GET, null, null, String.class));
        status(401, () -> client.login(new LoginRequest(ana, "incorrecta")));
        status(403, () -> client.modificarMascota(juan, clave, luna.id(), new MascotaRequest("Ajena", Sexo.HEMBRA, Tipo.PERRO)));
        status(403, () -> client.eliminarMascota(juan, clave, luna.id()));
        status(403, () -> client.candidatos(juan, clave, luna.id()));
        status(403, () -> client.votar(juan, clave, new VotoRequest(luna.id(), toby.id())));
        VotoDto voto = client.votar(ana, clave, new VotoRequest(luna.id(), toby.id()));
        status(403, () -> client.responder(ana, clave, voto.id()));
        String perfil = request("/usuarios/me", HttpMethod.GET, null, ana, String.class).getBody();
        assertThat(perfil).doesNotContain("clave", "$2a$", "contenido");
        status(404, () -> request("/mascotas/no-existe", HttpMethod.GET, null, ana, String.class));
    }

    @Test
    void validacionesYConflictos() {
        status(400, () -> client.crearMascota(ana, clave, new MascotaRequest(" ", Sexo.HEMBRA, Tipo.PERRO)));
        status(400, () -> client.crearMascota(ana, clave, new MascotaRequest("Luna", null, Tipo.PERRO)));
        status(400, () -> client.registrar(new UsuarioRequest("Ana", "Perez", "invalido", clave, clave, zona.id())));
        status(400, () -> client.registrar(new UsuarioRequest("Ana", "Perez", "otra@test.com", clave, "distinta", zona.id())));
        status(409, () -> client.registrar(new UsuarioRequest("Ana", "Perez", ana.toUpperCase(Locale.ROOT), clave, clave, zona.id())));
        status(404, () -> client.registrar(new UsuarioRequest("Ana", "Perez", "otra@test.com", clave, clave, "no-existe")));
        status(400, () -> client.votar(ana, clave, new VotoRequest(luna.id(), luna.id())));
        MascotaDto propia = client.crearMascota(ana, clave, new MascotaRequest("Rocky", Sexo.MACHO, Tipo.PERRO));
        status(400, () -> client.votar(ana, clave, new VotoRequest(luna.id(), propia.id())));
        client.votar(ana, clave, new VotoRequest(luna.id(), toby.id()));
        status(409, () -> client.votar(ana, clave, new VotoRequest(luna.id(), toby.id())));
    }

    @Test
    void candidatosFiltranTipoSexoZonaYBajas() {
        MascotaDto gato = client.crearMascota(juan, clave, new MascotaRequest("Michi", Sexo.MACHO, Tipo.GATO));
        MascotaDto hembra = client.crearMascota(juan, clave, new MascotaRequest("Lola", Sexo.HEMBRA, Tipo.PERRO));
        ZonaDto otra = client.crearZona(new ZonaRequest("Otra zona", ""));
        String pedro = "pedro." + UUID.randomUUID() + "@test.com";
        client.registrar(new UsuarioRequest("Pedro", "Perez", pedro, clave, clave, otra.id()));
        MascotaDto lejos = client.crearMascota(pedro, clave, new MascotaRequest("Rex", Sexo.MACHO, Tipo.PERRO));
        assertThat(client.candidatos(ana, clave, luna.id())).extracting(MascotaDto::id).containsExactly(toby.id());
        for (MascotaDto incompatible : List.of(gato, hembra, lejos)) {
            status(400, () -> client.votar(ana, clave, new VotoRequest(luna.id(), incompatible.id())));
        }
        client.eliminarMascota(juan, clave, toby.id());
        assertThat(client.candidatos(ana, clave, luna.id())).isEmpty();
    }

    @Test
    void editarBajaLogicaYRehabilitarMascota() {
        assertThat(client.modificarMascota(ana, clave, luna.id(), new MascotaRequest("Lunita", Sexo.HEMBRA, Tipo.PERRO)).nombre()).isEqualTo("Lunita");
        client.eliminarMascota(ana, clave, luna.id());
        assertThat(client.mascotas(ana, clave)).isEmpty();
        status(404, () -> request("/mascotas/" + luna.id(), HttpMethod.GET, null, ana, String.class));
        MascotaDto[] todas = request("/mascotas?incluirBajas=true", HttpMethod.GET, null, ana, MascotaDto[].class).getBody();
        assertThat(todas).hasSize(1); assertThat(todas[0].baja()).isNotNull();
        status(403, () -> request("/mascotas/" + luna.id() + "/habilitar", HttpMethod.PUT, null, juan, String.class));
        request("/mascotas/" + luna.id() + "/habilitar", HttpMethod.PUT, null, ana, MascotaDto.class);
        assertThat(client.mascotas(ana, clave)).hasSize(1);
    }

    @Test
    void modificarPerfilYCicloDeUsuario() {
        String nuevoMail = "nueva." + UUID.randomUUID() + "@test.com";
        String nuevaClave = "otraClave123";
        UsuarioDto perfil = request("/usuarios/me", HttpMethod.PUT,
                new UsuarioRequest("Anita", "Perez", nuevoMail, nuevaClave, nuevaClave, zona.id()), ana, UsuarioDto.class).getBody();
        assertThat(perfil.nombre()).isEqualTo("Anita");
        status(401, () -> client.login(new LoginRequest(ana, clave)));
        assertThat(client.login(new LoginRequest(nuevoMail, nuevaClave)).id()).isEqualTo(perfil.id());
        HttpHeaders headers = new HttpHeaders(); headers.setBasicAuth(nuevoMail, nuevaClave);
        rest.exchange(url + "/api/usuarios/me", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        status(401, () -> client.perfil(nuevoMail, nuevaClave));
        assertThat(client.candidatos(juan, clave, toby.id())).isEmpty();
        rest.exchange(url + "/api/usuarios/me/habilitar", HttpMethod.PUT, new HttpEntity<>(headers), UsuarioDto.class);
        assertThat(client.perfil(nuevoMail, nuevaClave).baja()).isNull();
        assertThat(client.candidatos(juan, clave, toby.id())).hasSize(1);
    }

    @Test
    void fotosMultipartPermisosYDescarga() {
        byte[] contenido = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1kAAAAASUVORK5CYII=");
        ByteArrayResource resource = new ByteArrayResource(contenido) {
            @Override public String getFilename() { return "foto.png"; }
        };
        HttpHeaders fileHeaders = new HttpHeaders(); fileHeaders.setContentType(MediaType.IMAGE_PNG);
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("archivo", new HttpEntity<>(resource, fileHeaders));
        HttpHeaders headers = auth(ana); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        FotoDto foto = rest.exchange(url + "/api/mascotas/" + luna.id() + "/foto", HttpMethod.PUT,
                new HttpEntity<>(form, headers), FotoDto.class).getBody();
        assertThat(foto.mime()).isEqualTo("image/png");
        ResponseEntity<byte[]> descarga = request("/fotos/" + foto.id(), HttpMethod.GET, null, juan, byte[].class);
        assertThat(descarga.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(descarga.getBody()).isEqualTo(contenido);
        HttpHeaders ajeno = auth(juan); ajeno.setContentType(MediaType.MULTIPART_FORM_DATA);
        status(403, () -> rest.exchange(url + "/api/mascotas/" + luna.id() + "/foto", HttpMethod.PUT,
                new HttpEntity<>(form, ajeno), FotoDto.class));
        fileHeaders.setContentType(MediaType.TEXT_PLAIN);
        form.set("archivo", new HttpEntity<>(resource, fileHeaders));
        status(400, () -> rest.exchange(url + "/api/usuarios/me/foto", HttpMethod.PUT,
                new HttpEntity<>(form, headers), FotoDto.class));
    }

    @Test
    void auditoriaConservaAltaModificacionYBaja() {
        client.modificarMascota(ana, clave, luna.id(), new MascotaRequest("Lunita", Sexo.HEMBRA, Tipo.PERRO));
        client.eliminarMascota(ana, clave, luna.id());
        transactions.executeWithoutResult(tx -> {
            var reader = AuditReaderFactory.get(entityManager);
            var revisions = reader.getRevisions(Mascota.class, luna.id());
            assertThat(revisions).hasSize(3);
            Mascota original = reader.find(Mascota.class, luna.id(), revisions.get(0));
            Mascota baja = reader.find(Mascota.class, luna.id(), revisions.get(2));
            assertThat(original.getNombre()).isEqualTo("Luna");
            assertThat(baja.getNombre()).isEqualTo("Lunita"); assertThat(baja.getBaja()).isNotNull();
        });
    }
}
