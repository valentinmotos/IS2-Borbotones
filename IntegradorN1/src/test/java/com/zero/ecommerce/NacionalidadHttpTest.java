package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifica el renderizado completo con el buffer real de Tomcat: en el primer acceso sin sesión el token
 * CSRF tiene que materializarse antes de enviar la página. Desde E1-01 el panel exige login, así que el
 * primer acceso es /login y después se pide el listado con la sesión autenticada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
class NacionalidadHttpTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Test
    void primerAccesoSinSesionYListadoCompletoConCsrf() throws Exception {
        // El CookieManager guarda el JSESSIONID entre requests, como un navegador.
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
            HttpResponse<String> login = client.send(HttpRequest.newBuilder(url("/login")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(login.headers().allValues("Set-Cookie")).anyMatch(cookie -> cookie.startsWith("JSESSIONID="));
            Matcher token = CSRF.matcher(login.body());
            assertThat(token.find()).isTrue();

            String formulario = "_csrf=" + codificar(token.group(1)) + "&username=" + codificar("admin@zero.com.ar")
                    + "&password=" + codificar("Admin123!");
            HttpResponse<String> ingreso = client.send(HttpRequest.newBuilder(url("/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(ingreso.statusCode()).isEqualTo(302);
            assertThat(ingreso.headers().firstValue("Location")).get().asString().endsWith("/admin");

            HttpResponse<String> listado = client.send(
                    HttpRequest.newBuilder(url("/admin/nacionalidades")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(listado.statusCode()).isEqualTo(200);
            assertThat(listado.body()).contains("name=\"_csrf\"", "Nacionalidades",
                    "/vendor/template/js/main.js", "</body>", "</html>");
        }
    }

    private URI url(String ruta) {
        return URI.create("http://127.0.0.1:" + port + ruta);
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
