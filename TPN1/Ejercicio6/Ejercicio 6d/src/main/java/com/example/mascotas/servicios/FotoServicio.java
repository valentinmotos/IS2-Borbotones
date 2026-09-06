package com.example.mascotas.servicios;

import com.example.mascotas.entidades.Foto;
import com.example.mascotas.errores.ErrorServicio;
import com.example.mascotas.repositorios.FotoRepositorio;
import jakarta.mail.Multipart;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.Set;

@Service
public class FotoServicio {

    private static final long TAMANIO_MAXIMO = 2 * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");

    @Autowired
    private FotoRepositorio fotoRepositorio;

    // cada vez que se ejecuta alguna de estas funciones se realiza una transaccion en la bd
    @Transactional
    public Foto guardar(MultipartFile archivo) throws ErrorServicio {
        if (archivo != null && !archivo.isEmpty()) {
            try {
                validarArchivo(archivo);
                Foto foto = new Foto();
                foto.setMime(archivo.getContentType());
                foto.setNombre(archivo.getName());
                foto.setContenido(archivo.getBytes());

                return fotoRepositorio.save(foto);
            } catch (ErrorServicio e) {
                throw e;
            } catch (Exception e) {
                throw new ErrorServicio("No fue posible guardar la foto");
            }
        }

        return null;
    }

    @Transactional
    public Foto actualizar (String idFoto, MultipartFile archivo) throws ErrorServicio {
        if (archivo == null || archivo.isEmpty()) {
            if (idFoto == null) return null;
            return fotoRepositorio.findById(idFoto)
                    .orElseThrow(() -> new ErrorServicio("No se encontro la foto a actualizar"));
        }
        if (archivo != null && !archivo.isEmpty()) {
            try {
                validarArchivo(archivo);
                Foto foto = new Foto();

                if (idFoto != null) {
                    Optional<Foto> respuesta = fotoRepositorio.findById(idFoto);
                    if (respuesta.isPresent()) {
                        foto = respuesta.get();
                    }
                }
                foto.setMime(archivo.getContentType());
                foto.setNombre(archivo.getName());
                foto.setContenido(archivo.getBytes());

                return fotoRepositorio.save(foto);
            } catch (ErrorServicio e) {
                throw e;
            } catch (Exception e) {
                throw new ErrorServicio("No fue posible actualizar la foto");
            }
        }

        return null;
    }

    private void validarArchivo(MultipartFile archivo) throws ErrorServicio {
        if (archivo.getSize() > TAMANIO_MAXIMO) throw new ErrorServicio("La foto no puede superar los 2 MB");
        if (!TIPOS_PERMITIDOS.contains(archivo.getContentType())) throw new ErrorServicio("Solo se admiten imágenes JPG o PNG");
    }
}
