package com.example.mascotas.servicios;

import com.example.mascotas.entidades.Foto;
import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.entidades.Zona;
import com.example.mascotas.errores.ErrorServicio;
import com.example.mascotas.repositorios.UsuarioRespositorio;
import com.example.mascotas.repositorios.ZonaRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Optional;

@Service
public class UsuarioServicio {

    //Se inicializa la variable automaticamente
    @Autowired
    private UsuarioRespositorio usuarioRepositorio;

    @Autowired
    private FotoServicio fotoServicio;

    @Autowired
    private NotificacionServicio notificacionServicio;

    @Autowired
    private ZonaRepositorio zonaRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // cada vez que se ejecuta alguna de estas funciones se realiza una transaccion en la bd
    @Transactional
    public void registrar(MultipartFile archivo, String nombre, String apellido, String mail, String clave, String clave2, String idZona) throws ErrorServicio {


        Zona zona = buscarZona(idZona);

        validar(nombre, apellido, mail, clave, clave2, zona);

        String mailNormalizado = normalizarMail(mail);
        if (usuarioRepositorio.buscarPorMail(mailNormalizado) != null) throw new ErrorServicio("Ya existe un usuario registrado con ese mail");
        Usuario usuario = new Usuario();
        usuario.setZona(zona);
        usuario.setNombre(nombre.trim());
        usuario.setApellido(apellido.trim());
        usuario.setMail(mailNormalizado);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setAlta(new Date());

        Foto foto = fotoServicio.guardar(archivo);
        usuario.setFoto(foto);

        usuarioRepositorio.save(usuario);

        //notificacionServicio.enviar("Bienvenido al tinder para mascotas", "Tinder de mascotas", usuario.getMail());
    }

    // cada vez que se ejecuta alguna de estas funciones se realiza una transaccion en la bd
    @Transactional
    public void modificar(MultipartFile archivo, String id, String nombre, String apellido, String mail, String clave, String clave2, String idZona) throws ErrorServicio {

        Zona zona = buscarZona(idZona);

        validar(nombre, apellido, mail, clave, clave2, zona);

        Optional<Usuario> respuesta  = usuarioRepositorio.findById(id);

        if (respuesta.isPresent()) {
            Usuario usuario = respuesta.get();
            String mailNormalizado = normalizarMail(mail);
            Usuario existente = usuarioRepositorio.buscarPorMail(mailNormalizado);
            if (existente != null && !existente.getId().equals(usuario.getId())) throw new ErrorServicio("Ya existe un usuario registrado con ese mail");
            usuario.setNombre(nombre.trim());
            usuario.setApellido(apellido.trim());
            usuario.setMail(mailNormalizado);
            usuario.setZona(zona);
            usuario.setClave(passwordEncoder.encode(clave));

            String idFoto = null;
            if(usuario.getFoto() != null) {
                idFoto = usuario.getFoto().getId();
            }

            Foto foto = fotoServicio.actualizar(idFoto, archivo);
            usuario.setFoto(foto);

            usuarioRepositorio.save(usuario);
        } else {
            throw new ErrorServicio("No se encontro el usuario solicitado");
        }
    }

    // cada vez que se ejecuta alguna de estas funciones se realiza una transaccion en la bd
    @Transactional
    public void deshabilitar(String id) throws ErrorServicio {
        Optional<Usuario> respuesta  = usuarioRepositorio.findById(id);

        if (respuesta.isPresent()) {
            Usuario usuario = respuesta.get();
            usuario.setBaja(new Date());

            usuarioRepositorio.save(usuario);
        } else {
            throw new ErrorServicio("No se encontro el usuario solicitado");
        }
    }

    // cada vez que se ejecuta alguna de estas funciones se realiza una transaccion en la bd
    @Transactional
    public void habilitar(String id) throws ErrorServicio {
        Optional<Usuario> respuesta  = usuarioRepositorio.findById(id);

        if (respuesta.isPresent()) {
            Usuario usuario = respuesta.get();
            usuario.setBaja(null);

            usuarioRepositorio.save(usuario);
        } else {
            throw new ErrorServicio("No se encontro el usuario solicitado");
        }
    }

    @Transactional()
    public Usuario buscarUsuario(String idUsuario) throws ErrorServicio {
        try {
            if (idUsuario == null || idUsuario.trim().isEmpty()) {
                throw new ErrorServicio("Debe indicar el usuario");
            }
            Optional<Usuario> optional = usuarioRepositorio.findById(idUsuario);
            if (optional.isEmpty() || optional.get().getBaja() != null) throw new ErrorServicio("No se encuentra el usuario indicado");
            return optional.get();

        } catch (ErrorServicio e) {
            throw e;
        }
    }

    public void validar(String nombre, String apellido, String mail, String clave, String clave2, Zona zona) throws ErrorServicio {
        if (nombre == null || !nombre.trim().matches("[A-Za-zÁÉÍÓÚáéíóúÑñ' -]{2,60}")) {
            throw new ErrorServicio("El nombre debe tener entre 2 y 60 caracteres válidos");
        }

        if (apellido == null || !apellido.trim().matches("[A-Za-zÁÉÍÓÚáéíóúÑñ' -]{2,60}")) {
            throw new ErrorServicio("El apellido debe tener entre 2 y 60 caracteres válidos");
        }

        if (mail == null || !mail.trim().matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ErrorServicio("Debe ingresar un mail válido");
        }

        if (clave == null || !clave.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}")) {
            throw new ErrorServicio("La clave debe tener entre 8 y 72 caracteres, mayúscula, minúscula y número");
        }

        if (!clave.equals(clave2)) {
            throw new ErrorServicio("Las claves deben ser iguales");
        }

        if (zona == null) {
            throw new ErrorServicio("No se encontro la zona solicitada");
        }
    }

    private Zona buscarZona(String idZona) throws ErrorServicio {
        if (idZona == null || idZona.isBlank()) throw new ErrorServicio("Debe seleccionar una zona");
        return zonaRepositorio.findById(idZona).orElseThrow(() -> new ErrorServicio("No se encontro la zona solicitada"));
    }

    private String normalizarMail(String mail) { return mail.trim().toLowerCase(); }

}
