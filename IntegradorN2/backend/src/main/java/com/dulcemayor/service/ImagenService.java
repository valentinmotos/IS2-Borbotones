package com.dulcemayor.service;

import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.dto.ImagenContenidoDTO;
import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.dto.mapper.ImagenMapper;
import com.dulcemayor.entity.Imagen;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.ImagenRepository;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.dulcemayor.entity.enums.TipoImagen;

@Service
public class ImagenService extends AbstractCrudService<Imagen, ImagenCargaDTO, ImagenDTO> {

    private static final long TAMANIO_MAXIMO = 2L * 1024 * 1024;

    public ImagenService(ImagenRepository imagenRepository, ImagenMapper imagenMapper) {
        super(imagenRepository, imagenMapper);
    }

    @Transactional(readOnly = true)
    public ImagenContenidoDTO buscarContenido(String id) {
        Imagen imagen = buscarEntidad(id);
        return new ImagenContenidoDTO(imagen.getMime(), imagen.getContenido());
    }

    /** Guarda una imagen que el seeder lee desde el classpath. */
    @Transactional
    public ImagenDTO crearImagen(String nombre, byte[] contenido, TipoImagen tipo) {
        if (contenido == null || contenido.length == 0 || contenido.length > TAMANIO_MAXIMO) {
            throw new ErrorServiceException("La imagen debe tener contenido y no superar los 2 MB.", "archivo");
        }
        if (tipo == null) throw new ErrorServiceException("El tipo de imagen es obligatorio.", "tipoImagen");
        Imagen imagen = new Imagen();
        imagen.setNombre(normalizarNombre(nombre));
        imagen.setMime(detectarMime(contenido));
        imagen.setContenido(contenido);
        imagen.setTipoImagen(tipo);
        return mapper.aRespuesta(repositorio.save(imagen));
    }

    @Override
    protected void validar(ImagenCargaDTO request, String idActual) {
        if (idActual != null) {
            throw new ErrorServiceException("Las imágenes no se pueden modificar.");
        }
        if (request == null || request.tipoImagen() == null) {
            throw new ErrorServiceException("El tipo de imagen es obligatorio.", "tipoImagen");
        }
        validarArchivo(request.archivo());
        detectarMime(leerContenido(request.archivo()));
    }

    @Override
    protected Imagen nuevaEntidad() {
        return new Imagen();
    }

    @Override
    protected void asignar(Imagen imagen, ImagenCargaDTO request) {
        byte[] contenido = leerContenido(request.archivo());
        String mime = detectarMime(contenido);
        imagen.setNombre(normalizarNombre(request.archivo().getOriginalFilename()));
        imagen.setMime(mime);
        imagen.setContenido(contenido);
        imagen.setTipoImagen(request.tipoImagen());
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "No se encontró la imagen.";
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ErrorServiceException("Seleccioná una imagen para subir.", "archivo");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new ErrorServiceException("La imagen no puede superar los 2 MB.", "archivo");
        }
    }

    private byte[] leerContenido(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new ErrorServiceException("No se pudo leer el archivo enviado.", "archivo");
        }
    }

    private String detectarMime(byte[] contenido) {
        if (contenido.length >= 3 && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8 && (contenido[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        byte[] firmaPng = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (contenido.length >= firmaPng.length
                && Arrays.equals(Arrays.copyOf(contenido, firmaPng.length), firmaPng)) {
            return "image/png";
        }
        if (contenido.length >= 12 && contenido[0] == 'R' && contenido[1] == 'I'
                && contenido[2] == 'F' && contenido[3] == 'F'
                && contenido[8] == 'W' && contenido[9] == 'E'
                && contenido[10] == 'B' && contenido[11] == 'P') {
            return "image/webp";
        }
        throw new ErrorServiceException("El archivo debe ser JPG, PNG o WEBP.", "archivo");
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "imagen";
        }
        String limpio = nombre.replace('\\', '/');
        limpio = limpio.substring(limpio.lastIndexOf('/') + 1).strip();
        if (limpio.length() > 200) {
            limpio = limpio.substring(limpio.length() - 200);
        }
        return limpio.isBlank() ? "imagen" : limpio;
    }
}
