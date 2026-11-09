package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.dulcemayor.service.GeocodificacionService.Resultado;
import com.sun.net.httpserver.HttpServer;

class GeocodificacionServiceTest {

    @Test
    void limitaConsultasConcurrentesYReusaElResultadoNormalizado() throws Exception {
        var solicitudes = new AtomicInteger();
        var ruta = new AtomicReference<String>();
        var agente = new AtomicReference<String>();
        var recibida = new CountDownLatch(1);
        var responder = new CountDownLatch(1);
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/search", intercambio -> {
            solicitudes.incrementAndGet();
            ruta.set(intercambio.getRequestURI().getQuery());
            agente.set(intercambio.getRequestHeaders().getFirst("User-Agent"));
            recibida.countDown();
            try {
                if (!responder.await(5, TimeUnit.SECONDS)) {
                    return;
                }
                var json = """
                        [{"display_name":"Av. España 10","lat":"-32.89","lon":"-68.84"},
                         {"display_name":"Inválida","lat":"91","lon":"-68.84"}]
                        """.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, json.length);
                intercambio.getResponseBody().write(json);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
        var servicio = new GeocodificacionService(RestClient.builder(),
                "http://127.0.0.1:" + servidor.getAddress().getPort());
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primera = executor.submit(() -> servicio.buscar("  Av. España  10, Mendoza  "));
            try {
                assertThat(recibida.await(3, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> servicio.buscar("Otra dirección, Mendoza"))
                        .isInstanceOfSatisfying(ResponseStatusException.class,
                                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
            } finally {
                responder.countDown();
            }

            assertThat(primera.get(3, TimeUnit.SECONDS))
                    .containsExactly(new Resultado("Av. España 10", -32.89, -68.84));
            assertThat(servicio.buscar("av. españa 10, Mendoza")).hasSize(1);
            assertThat(solicitudes.get()).isEqualTo(1);
            assertThat(ruta.get()).contains("q=Av. España 10, Mendoza", "countrycodes=ar");
            assertThat(agente.get()).startsWith("DulceMayor-IntegradorN2");
        } finally {
            servidor.stop(0);
        }
    }
}
