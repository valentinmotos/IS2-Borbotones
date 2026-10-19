package ar.edu.clinica.api;

import ar.edu.clinica.model.Medico;
import ar.edu.clinica.service.MedicoService;
import ar.edu.clinica.api.Solicitudes.Persona;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api/medicos")
public class MedicoController {
 private final MedicoService service;
 public MedicoController(MedicoService service) { this.service=service; }
 @GetMapping public List<Medico> listar() { return service.listar(); }
 @GetMapping("/{id}") public Medico buscar(@PathVariable String id) { return service.buscar(id); }
 @PostMapping public ResponseEntity<Medico> crear(@Valid @RequestBody Persona datos) { var e=service.crear(datos); return ResponseEntity.created(URI.create("/api/medicos/"+e.id)).body(e); }
 @PutMapping("/{id}") public Medico modificar(@PathVariable String id,@Valid @RequestBody Persona datos) { return service.modificar(id,datos); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable String id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
