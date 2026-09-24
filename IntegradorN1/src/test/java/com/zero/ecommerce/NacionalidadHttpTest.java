package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Verifica el renderizado completo con sesión nueva y el buffer real de Tomcat. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
class NacionalidadHttpTest {

    @LocalServerPort
    private int port;

    @Test
    void primerAccesoSinSesionEntregaListadoCompletoConCsrf() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/admin/nacionalidades")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().allValues("Set-Cookie"))
                    .anyMatch(cookie -> cookie.startsWith("JSESSIONID="));
            assertThat(response.body()).contains("name=\"_csrf\"", "Nacionalidades",
                    "/vendor/template/js/main.js", "</body>", "</html>");
        }
    }
}
