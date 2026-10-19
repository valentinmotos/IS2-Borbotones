package ar.edu.clinica.api;

import ar.edu.clinica.model.Paciente;
import ar.edu.clinica.service.PacienteService;
import ar.edu.clinica.api.Solicitudes.Persona;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api/pacientes")
public class PacienteController {
 private final PacienteService service;
 public PacienteController(PacienteService service) { this.service=service; }
 @GetMapping public List<Paciente> listar() { return service.listar(); }
 @GetMapping("/{id}") public Paciente buscar(@PathVariable String id) { return service.buscar(id); }
 @PostMapping public ResponseEntity<Paciente> crear(@Valid @RequestBody Persona datos) { var e=service.crear(datos); return ResponseEntity.created(URI.create("/api/pacientes/"+e.id)).body(e); }
 @PutMapping("/{id}") public Paciente modificar(@PathVariable String id,@Valid @RequestBody Persona datos) { return service.modificar(id,datos); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable String id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
