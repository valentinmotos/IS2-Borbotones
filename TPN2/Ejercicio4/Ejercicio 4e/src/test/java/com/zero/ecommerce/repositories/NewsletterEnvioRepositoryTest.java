package com.zero.ecommerce.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewsletterEnvioRepositoryTest {

    @TempDir Path temporal;

    @Test
    void persisteYRecuperaLaFechaEnArchivo() throws Exception {
        Path archivo = temporal.resolve("subdirectorio/newsletter-ultimo-envio.txt");
        NewsletterEnvioRepository repository = new NewsletterEnvioRepository(archivo.toString());
        LocalDate fecha = LocalDate.of(2026, 9, 26);

        assertThat(repository.buscarUltimoEnvio()).isEmpty();
        repository.guardarUltimoEnvio(fecha);

        assertThat(repository.buscarUltimoEnvio()).contains(fecha);
        assertThat(Files.readString(archivo)).isEqualTo("2026-09-26");
    }

    @Test
    void unArchivoInvalidoSeTrataComoSinEnvioPrevio() throws Exception {
        Path archivo = temporal.resolve("newsletter-ultimo-envio.txt");
        Files.writeString(archivo, "fecha-invalida");

        NewsletterEnvioRepository repository = new NewsletterEnvioRepository(archivo.toString());

        assertThat(repository.buscarUltimoEnvio()).isEmpty();
    }
}
