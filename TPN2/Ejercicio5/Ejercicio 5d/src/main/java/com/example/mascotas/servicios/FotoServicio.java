package com.example.mascotas.servicios;

import com.example.mascotas.entidades.*;
import com.example.mascotas.repositorios.FotoRepositorio;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import static com.example.mascotas.servicios.UsuarioServicio.error;

@Service
@Transactional
public class FotoServicio {
    private final FotoRepositorio fotos;
    private final MascotaServicio mascotas;
    private final UsuarioServicio usuarios;
    public FotoServicio(FotoRepositorio fotos, MascotaServicio mascotas, UsuarioServicio usuarios) {
        this.fotos=fotos; this.mascotas=mascotas; this.usuarios=usuarios;
    }
    public void subirMascota(String usuario, String id, MultipartFile archivo) {
        Mascota m=mascotas.propia(usuario,id); m.setFoto(guardar(archivo,m.getFoto()));
    }
    public void subirUsuario(String usuario, MultipartFile archivo) {
        Usuario u=usuarios.actual(usuario); u.setFoto(guardar(archivo,u.getFoto()));
    }
    private Foto guardar(MultipartFile archivo, Foto anterior) {
        if (archivo.isEmpty() || archivo.getSize()>2*1024*1024) throw error(HttpStatus.BAD_REQUEST,"Foto vacia o mayor de 2 MB");
        try (var entrada=ImageIO.createImageInputStream(new ByteArrayInputStream(archivo.getBytes()))) {
            var lectores=ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw error(HttpStatus.BAD_REQUEST,"Debe subir una imagen PNG o JPEG valida");
            var lector=lectores.next();
            try {
                String formato=lector.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!formato.equals("png") && !formato.equals("jpeg")) throw error(HttpStatus.BAD_REQUEST,"Solo se aceptan PNG y JPEG");
                lector.setInput(entrada);
                if ((long)lector.getWidth(0)*lector.getHeight(0)>16_000_000) throw error(HttpStatus.BAD_REQUEST,"La foto supera los 16 megapixeles");
                var imagen=lector.read(0);
                var salida=new ByteArrayOutputStream();
                ImageIO.write(imagen,formato,salida);
                Foto foto=anterior==null ? new Foto() : anterior;
                foto.setMime(formato.equals("png") ? "image/png" : "image/jpeg");
                foto.setNombre("foto."+formato); foto.setContenido(salida.toByteArray());
                return fotos.save(foto);
            } finally { lector.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw error(HttpStatus.BAD_REQUEST,"La imagen no se pudo leer");
        }
    }
    public ResponseEntity<byte[]> fotoMascota(String id) { return respuesta(mascotas.buscar(id).getFoto()); }
    public ResponseEntity<byte[]> fotoUsuario(String usuario) { return respuesta(usuarios.actual(usuario).getFoto()); }
    private ResponseEntity<byte[]> respuesta(Foto foto) {
        if (foto==null) throw error(HttpStatus.NOT_FOUND,"No hay foto cargada");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getMime()))
                .cacheControl(CacheControl.noStore()).body(foto.getContenido());
    }
}
