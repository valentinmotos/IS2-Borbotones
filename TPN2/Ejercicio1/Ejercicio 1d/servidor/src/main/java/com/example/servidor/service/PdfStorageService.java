package com.example.servidor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class PdfStorageService {
    private final Path directorio;

    public PdfStorageService(@Value("${app.biblioteca.directorio}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public String guardar(String titulo, MultipartFile pdf) {
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio para cargar un PDF");
        }
        String original = pdf.getOriginalFilename();
        try (var entrada = pdf.getInputStream()) {
            if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".pdf")
                    || !new String(entrada.readNBytes(5), StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser un PDF valido");
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el PDF", ex);
        }

        String nombre = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo debe contener letras o numeros");
        }
        nombre = "libro_" + nombre.substring(0, Math.min(nombre.length(), 150)) + "_.pdf";
        Path destino = resolver(nombre);
        boolean creado = false;
        try {
            Files.createDirectories(directorio);
            try (var salida = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW)) {
                creado = true;
                try (var entrada = pdf.getInputStream()) {
                    entrada.transferTo(salida);
                }
            }
            return nombre;
        } catch (FileAlreadyExistsException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un PDF para ese titulo; use otro titulo", ex);
        } catch (IOException ex) {
            if (creado) {
                eliminar(nombre);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el PDF en la biblioteca", ex);
        }
    }

    public Resource abrir(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este libro no tiene PDF");
        }
        try {
            Path archivo = resolver(nombre);
            if (!Files.isRegularFile(archivo) || !Files.isReadable(archivo)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF no encontrado");
            }
            return new UrlResource(archivo.toUri());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo abrir el PDF", ex);
        }
    }

    public void eliminar(String nombre) {
        if (nombre != null) {
            try {
                Files.deleteIfExists(resolver(nombre));
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar el PDF", ex);
            }
        }
    }

    private Path resolver(String nombre) {
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.getParent().equals(directorio) || !nombre.matches("libro_[a-z0-9_]+_\\.pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de PDF invalido");
        }
        return archivo;
    }
}
