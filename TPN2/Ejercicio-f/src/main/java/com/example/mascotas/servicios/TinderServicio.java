package com.example.mascotas.servicios;

import com.example.mascotas.dto.Dto.*;
import com.example.mascotas.entidades.*;
import com.example.mascotas.errores.ApiException;
import com.example.mascotas.mapper.MascotasMapper;
import com.example.mascotas.repositorios.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class TinderServicio {
    private final ZonaRepositorio zonas;
    private final UsuarioRepositorio usuarios;
    private final MascotaRepositorio mascotas;
    private final VotoRepositorio votos;
    private final FotoRepositorio fotos;
    private final MascotasMapper mapper;
    private final PasswordEncoder encoder;

    public TinderServicio(ZonaRepositorio zonas, UsuarioRepositorio usuarios, MascotaRepositorio mascotas,
            VotoRepositorio votos, FotoRepositorio fotos, MascotasMapper mapper, PasswordEncoder encoder) {
        this.zonas = zonas; this.usuarios = usuarios; this.mascotas = mascotas;
        this.votos = votos; this.fotos = fotos; this.mapper = mapper; this.encoder = encoder;
    }

    private ApiException error(HttpStatus status, String message) { return new ApiException(status, message); }

    @Transactional(readOnly = true)
    public List<ZonaDto> zonas() { return zonas.findAll().stream().map(mapper::zona).toList(); }
    @Transactional(readOnly = true)
    public ZonaDto buscarZona(String id) { return mapper.zona(zona(id)); }
    public ZonaDto crearZona(ZonaRequest request) { return mapper.zona(zonas.save(mapper.zona(request))); }

    private Zona zona(String id) {
        return zonas.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Zona inexistente"));
    }

    private Usuario credenciales(String mail, String clave, boolean permitirBaja) {
        Usuario usuario = usuarios.findByMail(mail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        if (!encoder.matches(clave, usuario.getClave())) throw error(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        if (!permitirBaja && usuario.getBaja() != null) throw error(HttpStatus.UNAUTHORIZED, "Usuario deshabilitado");
        return usuario;
    }

    private Usuario autenticar(String authorization, boolean permitirBaja) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6))
            throw error(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion HTTP Basic");
        String decoded;
        try { decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { throw error(HttpStatus.UNAUTHORIZED, "Cabecera Authorization invalida"); }
        int colon = decoded.indexOf(':');
        if (colon < 1) throw error(HttpStatus.UNAUTHORIZED, "Cabecera Authorization invalida");
        return credenciales(decoded.substring(0, colon), decoded.substring(colon + 1), permitirBaja);
    }

    private Usuario autenticar(String authorization) { return autenticar(authorization, false); }

    private void actualizarUsuario(Usuario usuario, UsuarioRequest request) {
        if (!request.clave().equals(request.clave2())) throw error(HttpStatus.BAD_REQUEST, "Las claves deben coincidir");
        if (request.clave().getBytes(StandardCharsets.UTF_8).length > 72)
            throw error(HttpStatus.BAD_REQUEST, "La clave no puede superar 72 bytes UTF-8");
        String mail = request.mail().trim().toLowerCase(Locale.ROOT);
        usuarios.findByMail(mail).filter(other -> !Objects.equals(other.getId(), usuario.getId()))
                .ifPresent(other -> { throw error(HttpStatus.CONFLICT, "El mail ya esta registrado"); });
        usuario.setNombre(request.nombre().trim()); usuario.setApellido(request.apellido().trim());
        usuario.setMail(mail); usuario.setClave(encoder.encode(request.clave())); usuario.setZona(zona(request.zonaId()));
    }

    public UsuarioDto registrar(UsuarioRequest request) {
        Usuario usuario = new Usuario(); actualizarUsuario(usuario, request); usuario.setAlta(new Date());
        return mapper.usuario(usuarios.save(usuario));
    }
    @Transactional(readOnly = true)
    public UsuarioDto login(LoginRequest request) { return mapper.usuario(credenciales(request.mail(), request.clave(), false)); }
    @Transactional(readOnly = true)
    public UsuarioDto perfil(String auth) { return mapper.usuario(autenticar(auth)); }
    public UsuarioDto modificarPerfil(String auth, UsuarioRequest request) {
        Usuario usuario = autenticar(auth); actualizarUsuario(usuario, request); return mapper.usuario(usuario);
    }
    public void deshabilitar(String auth) { autenticar(auth).setBaja(new Date()); }
    public UsuarioDto habilitar(String auth) {
        Usuario usuario = autenticar(auth, true); usuario.setBaja(null); return mapper.usuario(usuario);
    }

    private Mascota mascota(String id) {
        Mascota mascota = mascotas.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mascota inexistente"));
        if (mascota.getBaja() != null || mascota.getUsuario().getBaja() != null)
            throw error(HttpStatus.NOT_FOUND, "Mascota no disponible");
        return mascota;
    }
    private void propietario(Usuario usuario, Mascota mascota) {
        if (!usuario.getId().equals(mascota.getUsuario().getId())) throw error(HttpStatus.FORBIDDEN, "La mascota pertenece a otro usuario");
    }
    public MascotaDto crearMascota(String auth, MascotaRequest request) {
        Usuario usuario = autenticar(auth);
        Mascota mascota = mapper.mascota(request); mascota.setNombre(request.nombre().trim());
        mascota.setUsuario(usuario); mascota.setAlta(new Date()); return mapper.mascota(mascotas.save(mascota));
    }
    @Transactional(readOnly = true)
    public MascotaDto buscarMascota(String auth, String id) { autenticar(auth); return mapper.mascota(mascota(id)); }
    @Transactional(readOnly = true)
    public List<MascotaDto> misMascotas(String auth, boolean incluirBajas) {
        Usuario usuario = autenticar(auth);
        return mascotas.findAll().stream().filter(m -> m.getUsuario().getId().equals(usuario.getId()))
                .filter(m -> incluirBajas || m.getBaja() == null).map(mapper::mascota).toList();
    }
    @Transactional(readOnly = true)
    public List<MascotaDto> explorar(String auth, String origenId) {
        Usuario usuario = autenticar(auth); Mascota origen = mascota(origenId); propietario(usuario, origen);
        return mascotas.findAll().stream().filter(m -> m.getBaja() == null && m.getUsuario().getBaja() == null)
                .filter(m -> !m.getUsuario().getId().equals(usuario.getId()))
                .filter(m -> m.getTipo() == origen.getTipo() && m.getSexo() != origen.getSexo())
                .filter(m -> m.getUsuario().getZona().getId().equals(usuario.getZona().getId()))
                .filter(m -> !votos.existsByMascota1IdAndMascota2Id(origenId, m.getId()))
                .map(mapper::mascota).toList();
    }
    public MascotaDto modificarMascota(String auth, String id, MascotaRequest request) {
        Usuario usuario = autenticar(auth); Mascota mascota = mascota(id); propietario(usuario, mascota);
        mascota.setNombre(request.nombre().trim()); mascota.setSexo(request.sexo()); mascota.setTipo(request.tipo());
        return mapper.mascota(mascota);
    }
    public void eliminarMascota(String auth, String id) {
        Usuario usuario = autenticar(auth); Mascota mascota = mascota(id); propietario(usuario, mascota); mascota.setBaja(new Date());
    }
    public MascotaDto habilitarMascota(String auth, String id) {
        Usuario usuario = autenticar(auth);
        Mascota mascota = mascotas.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mascota inexistente"));
        propietario(usuario, mascota); mascota.setBaja(null); return mapper.mascota(mascota);
    }

    public VotoDto votar(String auth, VotoRequest request) {
        Usuario usuario = autenticar(auth);
        if (request.mascota1Id().equals(request.mascota2Id())) throw error(HttpStatus.BAD_REQUEST, "No puede autovotarse");
        Mascota origen = mascota(request.mascota1Id()); Mascota destino = mascota(request.mascota2Id());
        propietario(usuario, origen);
        if (destino.getUsuario().getId().equals(usuario.getId())) throw error(HttpStatus.BAD_REQUEST, "Debe votar mascotas de otro usuario");
        if (origen.getTipo() != destino.getTipo() || origen.getSexo() == destino.getSexo()
                || !origen.getUsuario().getZona().getId().equals(destino.getUsuario().getZona().getId()))
            throw error(HttpStatus.BAD_REQUEST, "Las mascotas deben ser del mismo tipo y zona, y de distinto sexo");
        if (votos.existsByMascota1IdAndMascota2Id(origen.getId(), destino.getId())) throw error(HttpStatus.CONFLICT, "El voto ya existe");
        Voto voto = new Voto(); voto.setMascota1(origen); voto.setMascota2(destino); voto.setFecha(new Date());
        return mapper.voto(votos.save(voto));
    }
    private boolean disponible(Voto voto) {
        return voto.getMascota1().getBaja() == null && voto.getMascota2().getBaja() == null
                && voto.getMascota1().getUsuario().getBaja() == null && voto.getMascota2().getUsuario().getBaja() == null;
    }
    public VotoDto responder(String auth, String id) {
        Usuario usuario = autenticar(auth);
        Voto voto = votos.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Voto inexistente"));
        propietario(usuario, voto.getMascota2());
        if (!disponible(voto)) throw error(HttpStatus.CONFLICT, "Las mascotas del voto no estan disponibles");
        if (voto.getRespuesta() == null) voto.setRespuesta(new Date());
        return mapper.voto(voto);
    }
    @Transactional(readOnly = true)
    public List<VotoDto> recibidos(String auth) {
        Usuario usuario = autenticar(auth);
        return votos.findAll().stream().filter(this::disponible)
                .filter(v -> v.getMascota2().getUsuario().getId().equals(usuario.getId())).map(mapper::voto).toList();
    }
    @Transactional(readOnly = true)
    public VotoDto buscarVoto(String auth, String id) {
        Usuario usuario = autenticar(auth);
        Voto voto = votos.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Voto inexistente"));
        if (!voto.getMascota1().getUsuario().getId().equals(usuario.getId())
                && !voto.getMascota2().getUsuario().getId().equals(usuario.getId()))
            throw error(HttpStatus.FORBIDDEN, "El voto pertenece a otros usuarios");
        return mapper.voto(voto);
    }
    @Transactional(readOnly = true)
    public List<VotoDto> matches(String auth) {
        Usuario usuario = autenticar(auth);
        return votos.findAll().stream().filter(this::disponible).filter(v -> v.getRespuesta() != null)
                .filter(v -> v.getMascota1().getUsuario().getId().equals(usuario.getId())
                        || v.getMascota2().getUsuario().getId().equals(usuario.getId())).map(mapper::voto).toList();
    }

    private Foto guardarFoto(Foto foto, MultipartFile archivo) {
        if (archivo.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "La foto no puede estar vacia");
        if (!Set.of("image/jpeg", "image/png", "image/gif", "image/webp").contains(archivo.getContentType()))
            throw error(HttpStatus.BAD_REQUEST, "Formato de imagen no permitido");
        if (foto == null) foto = new Foto();
        foto.setMime(archivo.getContentType()); foto.setNombre(archivo.getOriginalFilename());
        try { foto.setContenido(archivo.getBytes()); }
        catch (IOException ex) { throw error(HttpStatus.BAD_REQUEST, "No se pudo leer la foto"); }
        return fotos.save(foto);
    }
    public FotoDto fotoUsuario(String auth, MultipartFile archivo) {
        Usuario usuario = autenticar(auth); usuario.setFoto(guardarFoto(usuario.getFoto(), archivo)); return mapper.foto(usuario.getFoto());
    }
    public FotoDto fotoMascota(String auth, String id, MultipartFile archivo) {
        Usuario usuario = autenticar(auth); Mascota mascota = mascota(id); propietario(usuario, mascota);
        mascota.setFoto(guardarFoto(mascota.getFoto(), archivo)); return mapper.foto(mascota.getFoto());
    }
    public record Imagen(String mime, byte[] contenido) { }
    @Transactional(readOnly = true)
    public Imagen imagen(String auth, String id) {
        autenticar(auth); Foto foto = fotos.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Foto inexistente"));
        return new Imagen(foto.getMime(), foto.getContenido());
    }
}
