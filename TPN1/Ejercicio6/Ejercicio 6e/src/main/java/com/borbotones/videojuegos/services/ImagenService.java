package com.borbotones.videojuegos.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ImagenService {
    private static final long TAMANIO_MAXIMO = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    private final Path directorio;

    public ImagenService(@Value("${app.imagenes.directorio}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo, boolean requerido) {
        if (archivo == null || archivo.isEmpty()) {
            if (requerido) {
                throw new IllegalArgumentException("La imagen es obligatoria");
            }
            return null;
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB");
        }

        String extension = EXTENSIONES_PERMITIDAS.get(archivo.getContentType());
        if (extension == null || !esImagenReal(archivo)) {
            throw new IllegalArgumentException("Solo se admiten imagenes JPG o PNG validas");
        }

        String nombreSeguro = UUID.randomUUID() + extension;
        Path destino = directorio.resolve(nombreSeguro).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido");
        }

        try {
            Files.createDirectories(directorio);
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return nombreSeguro;
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible guardar la imagen", ex);
        }
    }

    private boolean esImagenReal(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            BufferedImage imagen = ImageIO.read(entrada);
            if (imagen == null || imagen.getWidth() <= 0 || imagen.getHeight() <= 0) {
                return false;
            }
            long pixeles = (long) imagen.getWidth() * imagen.getHeight();
            return imagen.getWidth() <= 8_000 && imagen.getHeight() <= 8_000 && pixeles <= 40_000_000;
        } catch (IOException ex) {
            return false;
        }
    }
}
