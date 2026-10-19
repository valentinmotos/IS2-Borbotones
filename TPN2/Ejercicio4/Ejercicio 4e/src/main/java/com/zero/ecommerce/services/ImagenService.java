package com.zero.ecommerce.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ImagenRepository;

@Service
@Transactional(readOnly = true)
public class ImagenService {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final List<String> FORMATOS_PERMITIDOS = List.of("image/jpeg", "image/png", "image/webp");

    private final ImagenRepository imagenRepository;

    public ImagenService(ImagenRepository imagenRepository) {
        this.imagenRepository = imagenRepository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Imagen crearImagen(MultipartFile archivo, TipoImagen tipoImagen) throws ErrorServiceException {
        validarArchivo(archivo, false, null);
        return guardarImagen(archivo, tipoImagen, null);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Imagen modificarImagen(String id, MultipartFile archivo, TipoImagen tipoImagen) throws ErrorServiceException {
        Imagen imagenActual = buscarImagen(id);
        validarArchivo(archivo, true, imagenActual);
        if (archivo == null || archivo.isEmpty()) {
            return imagenActual;
        }
        return guardarImagen(archivo, tipoImagen, imagenActual);
    }

    public Imagen buscarImagen(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El identificador de la imagen es inválido.");
        }
        return imagenRepository.findById(id)
                .orElseThrow(() -> new ErrorServiceException("La imagen no existe o fue eliminada."));
    }

    public Optional<Imagen> buscarImagenOpcional(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return imagenRepository.findById(id);
    }

    public byte[] obtenerContenido(String id) throws ErrorServiceException {
        return buscarImagen(id).getContenido();
    }

    public String obtenerMime(String id) throws ErrorServiceException {
        return buscarImagen(id).getMime();
    }

    private void validarArchivo(MultipartFile archivo, boolean permiteVacio, Imagen imagenActual) throws ErrorServiceException {
        if (archivo == null) {
            if (permiteVacio) {
                return;
            }
            throw new ErrorServiceException("Debe seleccionar una imagen válida.");
        }

        if (archivo.isEmpty()) {
            if (permiteVacio) {
                return;
            }
            throw new ErrorServiceException("La imagen seleccionada está vacía.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ErrorServiceException("La imagen supera el tamaño máximo permitido de 2 MB.");
        }

        String mime = detectarMime(archivo);
        if (mime == null || !FORMATOS_PERMITIDOS.contains(mime)) {
            throw new ErrorServiceException("El formato de la imagen no está permitido. Utilizá JPG, PNG o WEBP.");
        }

        if (archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()) {
            throw new ErrorServiceException("Debe seleccionar una imagen válida.");
        }

        if (imagenActual != null && archivo.getSize() == 0) {
            return;
        }
    }

    private Imagen guardarImagen(MultipartFile archivo, TipoImagen tipoImagen, Imagen imagenActual) throws ErrorServiceException {
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        String mime = detectarMime(archivo);

        try {
            byte[] contenido = archivo.getBytes();
            if (contenido == null || contenido.length == 0) {
                throw new ErrorServiceException("La imagen seleccionada está vacía.");
            }

            Imagen imagen = imagenActual != null ? imagenActual : new Imagen();
            imagen.setNombre(nombre);
            imagen.setMime(mime);
            imagen.setContenido(contenido);
            imagen.setTipoImagen(tipoImagen != null ? tipoImagen : TipoImagen.PRODUCTO);
            return imagenRepository.save(imagen);
        } catch (IOException e) {
            throw new ErrorServiceException("No se pudo leer el contenido de la imagen.");
        }
    }

    private String limpiarNombre(String nombre) {
        if (nombre == null) {
            return "imagen";
        }
        String limpio = nombre.replace("\\", "/").trim();
        int ultimaBarra = Math.max(limpio.lastIndexOf('/'), limpio.lastIndexOf('\\'));
        String archivo = ultimaBarra >= 0 ? limpio.substring(ultimaBarra + 1) : limpio;
        return archivo.isBlank() ? "imagen" : archivo;
    }

    private String detectarMime(MultipartFile archivo) {
        if (archivo == null || archivo.getOriginalFilename() == null) {
            return null;
        }

        String mime = archivo.getContentType();
        if (mime != null && FORMATOS_PERMITIDOS.contains(mime.toLowerCase())) {
            return mime.toLowerCase();
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> null;
        };
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return "";
        }
        int ultimoPunto = nombreArchivo.lastIndexOf('.') ;
        if (ultimoPunto < 0 || ultimoPunto == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(ultimoPunto + 1).toLowerCase();
    }
}
