package com.example.mascotas.controladores;

import com.example.mascotas.dto.Dto.*;
import com.example.mascotas.servicios.TinderServicio;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class TinderControlador {
    private final TinderServicio servicio;
    public TinderControlador(TinderServicio servicio) { this.servicio = servicio; }

    private <T> ResponseEntity<T> creado(String path, String id, T dto) {
        return ResponseEntity.created(URI.create("/api/" + path + "/" + id)).body(dto);
    }
    @GetMapping("/zonas")
    public List<ZonaDto> zonas() { return servicio.zonas(); }
    @GetMapping("/zonas/{id}")
    public ZonaDto buscarZona(@PathVariable String id) { return servicio.buscarZona(id); }
    @PostMapping("/zonas")
    public ResponseEntity<ZonaDto> zona(@Valid @RequestBody ZonaRequest request) {
        ZonaDto dto = servicio.crearZona(request); return creado("zonas", dto.id(), dto);
    }
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioDto dto = servicio.registrar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/me")).body(dto);
    }
    @PostMapping("/auth/login")
    public UsuarioDto login(@Valid @RequestBody LoginRequest request) { return servicio.login(request); }
    @GetMapping("/usuarios/me")
    public UsuarioDto perfil(@RequestHeader(value = "Authorization", required = false) String auth) { return servicio.perfil(auth); }
    @PutMapping("/usuarios/me")
    public UsuarioDto modificarPerfil(@RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody UsuarioRequest request) { return servicio.modificarPerfil(auth, request); }
    @DeleteMapping("/usuarios/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bajaUsuario(@RequestHeader(value = "Authorization", required = false) String auth) { servicio.deshabilitar(auth); }
    @PutMapping("/usuarios/me/habilitar")
    public UsuarioDto habilitarUsuario(@RequestHeader(value = "Authorization", required = false) String auth) { return servicio.habilitar(auth); }
    @PutMapping(value = "/usuarios/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoDto fotoUsuario(@RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam MultipartFile archivo) { return servicio.fotoUsuario(auth, archivo); }

    @GetMapping("/mascotas")
    public List<MascotaDto> propias(@RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(defaultValue = "false") boolean incluirBajas) { return servicio.misMascotas(auth, incluirBajas); }
    @PostMapping("/mascotas")
    public ResponseEntity<MascotaDto> crearMascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody MascotaRequest request) {
        MascotaDto dto = servicio.crearMascota(auth, request); return creado("mascotas", dto.id(), dto);
    }
    @GetMapping("/mascotas/{id}")
    public MascotaDto mascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { return servicio.buscarMascota(auth, id); }
    @PutMapping("/mascotas/{id}")
    public MascotaDto modificarMascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id, @Valid @RequestBody MascotaRequest request) { return servicio.modificarMascota(auth, id, request); }
    @DeleteMapping("/mascotas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bajaMascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { servicio.eliminarMascota(auth, id); }
    @PutMapping("/mascotas/{id}/habilitar")
    public MascotaDto habilitarMascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { return servicio.habilitarMascota(auth, id); }
    @GetMapping("/mascotas/{id}/candidatos")
    public List<MascotaDto> explorar(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { return servicio.explorar(auth, id); }
    @PutMapping(value = "/mascotas/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoDto fotoMascota(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id, @RequestParam MultipartFile archivo) { return servicio.fotoMascota(auth, id, archivo); }
    @GetMapping("/fotos/{id}")
    public ResponseEntity<byte[]> foto(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) {
        TinderServicio.Imagen imagen = servicio.imagen(auth, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagen.mime())).body(imagen.contenido());
    }
    @PostMapping("/votos")
    public ResponseEntity<VotoDto> votar(@RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody VotoRequest request) {
        VotoDto dto = servicio.votar(auth, request); return creado("votos", dto.id(), dto);
    }
    @GetMapping("/votos/recibidos")
    public List<VotoDto> recibidos(@RequestHeader(value = "Authorization", required = false) String auth) { return servicio.recibidos(auth); }
    @GetMapping("/votos/{id}")
    public VotoDto buscarVoto(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { return servicio.buscarVoto(auth, id); }
    @PutMapping("/votos/{id}/respuesta")
    public VotoDto responder(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) { return servicio.responder(auth, id); }
    @GetMapping("/matches")
    public List<VotoDto> matches(@RequestHeader(value = "Authorization", required = false) String auth) { return servicio.matches(auth); }
}
