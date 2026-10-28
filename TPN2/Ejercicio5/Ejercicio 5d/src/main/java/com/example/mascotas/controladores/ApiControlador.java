package com.example.mascotas.controladores;

import com.example.mascotas.dto.Datos.*;
import com.example.mascotas.repositorios.ZonaRepositorio;
import com.example.mascotas.servicios.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiControlador {
    private final UsuarioServicio usuarios;
    private final MascotaServicio mascotas;
    private final FotoServicio fotos;
    private final ZonaRepositorio zonas;
    public ApiControlador(UsuarioServicio usuarios, MascotaServicio mascotas, FotoServicio fotos, ZonaRepositorio zonas) {
        this.usuarios=usuarios; this.mascotas=mascotas; this.fotos=fotos; this.zonas=zonas;
    }
    @GetMapping("/zonas")
    List<ZonaVista> zonas() { return zonas.findAll().stream().map(ZonaVista::de).toList(); }
    @PostMapping("/auth/registro") @ResponseStatus(HttpStatus.CREATED)
    AuthRespuesta registro(@Valid @RequestBody Registro entrada) { return usuarios.registrar(entrada); }
    @PostMapping("/auth/login")
    AuthRespuesta login(@Valid @RequestBody Login entrada) { return usuarios.login(entrada); }
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(Authentication auth) { usuarios.logout(auth.getName()); }
    @GetMapping("/usuarios/me")
    UsuarioVista perfil(Authentication auth) { return usuarios.perfil(auth.getName()); }
    @PutMapping("/usuarios/me")
    UsuarioVista perfil(Authentication auth, @Valid @RequestBody Perfil entrada) { return usuarios.actualizar(auth.getName(),entrada); }
    @GetMapping("/mascotas")
    List<MascotaVista> propias(Authentication auth) { return mascotas.propias(auth.getName()); }
    @PostMapping("/mascotas") @ResponseStatus(HttpStatus.CREATED)
    MascotaVista crear(Authentication auth, @Valid @RequestBody MascotaEntrada entrada) { return mascotas.crear(auth.getName(),entrada); }
    @PutMapping("/mascotas/{id}")
    MascotaVista editar(Authentication auth, @PathVariable String id, @Valid @RequestBody MascotaEntrada entrada) { return mascotas.editar(auth.getName(),id,entrada); }
    @DeleteMapping("/mascotas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void baja(Authentication auth, @PathVariable String id) { mascotas.baja(auth.getName(),id); }
    @GetMapping("/mascotas/explorar")
    List<MascotaVista> explorar(Authentication auth, @RequestParam String idMascotaPropia) { return mascotas.explorar(auth.getName(),idMascotaPropia); }
    @PostMapping("/votos") @ResponseStatus(HttpStatus.CREATED)
    VotoVista votar(Authentication auth, @Valid @RequestBody VotoEntrada entrada) { return mascotas.votar(auth.getName(),entrada); }
    @GetMapping("/votos")
    List<VotoVista> votos(Authentication auth) { return mascotas.listarVotos(auth.getName()); }
    @PostMapping("/votos/{id}/responder")
    VotoVista responder(Authentication auth, @PathVariable String id) { return mascotas.responder(auth.getName(),id); }
    @PutMapping(value="/mascotas/{id}/foto", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.NO_CONTENT)
    void fotoMascota(Authentication auth, @PathVariable String id, @RequestParam MultipartFile archivo) { fotos.subirMascota(auth.getName(),id,archivo); }
    @GetMapping("/mascotas/{id}/foto")
    ResponseEntity<byte[]> fotoMascota(@PathVariable String id) { return fotos.fotoMascota(id); }
    @PutMapping(value="/usuarios/me/foto", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.NO_CONTENT)
    void fotoUsuario(Authentication auth, @RequestParam MultipartFile archivo) { fotos.subirUsuario(auth.getName(),archivo); }
    @GetMapping("/usuarios/me/foto")
    ResponseEntity<byte[]> fotoUsuario(Authentication auth) { return fotos.fotoUsuario(auth.getName()); }
}
