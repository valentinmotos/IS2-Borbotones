package com.zero.ecommerce.repositories;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.zero.ecommerce.exception.ErrorServiceException;

/** DAO de la fecha del newsletter. Persiste en archivo para no alterar el modelo de datos. */
@Repository
public class NewsletterEnvioRepository {

    private static final Logger log = LoggerFactory.getLogger(NewsletterEnvioRepository.class);
    private final Path archivo;

    public NewsletterEnvioRepository(
            @Value("${newsletter.archivo-ultimo-envio:data/newsletter-ultimo-envio.txt}") String archivo) {
        this.archivo = Path.of(archivo).toAbsolutePath().normalize();
    }

    public Optional<LocalDate> buscarUltimoEnvio() {
        if (!Files.exists(archivo)) {
            return Optional.empty();
        }
        try {
            String contenido = Files.readString(archivo, StandardCharsets.UTF_8).strip();
            return contenido.isEmpty() ? Optional.empty() : Optional.of(LocalDate.parse(contenido));
        } catch (IOException | DateTimeParseException e) {
            log.warn("No se pudo leer la fecha del newsletter en {}: {}", archivo, e.getMessage());
            return Optional.empty();
        }
    }

    public void guardarUltimoEnvio(LocalDate fecha) throws ErrorServiceException {
        if (fecha == null) {
            throw new ErrorServiceException("La fecha del último envío es obligatoria.");
        }
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        try {
            Path directorio = archivo.getParent();
            if (directorio != null) {
                Files.createDirectories(directorio);
            }
            Files.writeString(temporal, fecha.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveNoDisponible) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ErrorServiceException("No se pudo guardar la fecha del newsletter.");
        }
    }
}
