package ar.edu.clinica.api;

import ar.edu.clinica.model.Usuario;
import ar.edu.clinica.service.UsuarioService;
import ar.edu.clinica.api.Solicitudes.Cuenta;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api/usuarios")
public class UsuarioController {
 private final UsuarioService service;
 public UsuarioController(UsuarioService service) { this.service=service; }
 @GetMapping public List<Usuario> listar() { return service.listar(); }
 @GetMapping("/{id}") public Usuario buscar(@PathVariable String id) { return service.buscar(id); }
 @PostMapping public ResponseEntity<Usuario> crear(@Valid @RequestBody Cuenta datos) { var e=service.crear(datos); return ResponseEntity.created(URI.create("/api/usuarios/"+e.id)).body(e); }
 @PutMapping("/{id}") public Usuario modificar(@PathVariable String id,@Valid @RequestBody Cuenta datos) { return service.modificar(id,datos); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable String id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
