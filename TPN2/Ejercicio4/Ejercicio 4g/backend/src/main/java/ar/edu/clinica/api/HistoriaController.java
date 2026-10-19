package ar.edu.clinica.api;

import ar.edu.clinica.model.*;
import ar.edu.clinica.service.*;
import ar.edu.clinica.api.Solicitudes.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api/historias")
public class HistoriaController {
 private final HistoriaService historias; private final DetalleService detalles; private final HistoriaClinicaFacade facade;
 public HistoriaController(HistoriaService h,DetalleService d,HistoriaClinicaFacade f) { historias=h; detalles=d; facade=f; }
 @GetMapping public List<HistoriaClinica> listar() { return historias.listar(); }
 @GetMapping("/{id}") public HistoriaClinicaFacade.Expediente consultar(@PathVariable String id) { return facade.consultar(id); }
 @PostMapping public ResponseEntity<HistoriaClinica> crear(@Valid @RequestBody Historia datos) { var h=historias.crear(datos); return ResponseEntity.created(URI.create("/api/historias/"+h.id)).body(h); }
 @PutMapping("/{id}") public HistoriaClinica modificar(@PathVariable String id,@Valid @RequestBody Historia datos) { return historias.modificar(id,datos); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable String id) { historias.eliminar(id); return ResponseEntity.noContent().build(); }
 @GetMapping("/{id}/detalles") public List<DetalleHistoriaClinica> listarDetalles(@PathVariable String id) { return detalles.deHistoria(id); }
 @GetMapping("/{id}/detalles/{detalleId}") public DetalleHistoriaClinica detalle(@PathVariable String id,@PathVariable String detalleId) { return detalles.deHistoria(id,detalleId); }
 @PostMapping("/{id}/detalles") public ResponseEntity<DetalleHistoriaClinica> agregar(@PathVariable String id,@Valid @RequestBody Detalle datos) { var d=detalles.agregar(id,datos); return ResponseEntity.created(URI.create("/api/historias/"+id+"/detalles/"+d.id)).body(d); }
 @PutMapping("/{id}/detalles/{detalleId}") public DetalleHistoriaClinica modificarDetalle(@PathVariable String id,@PathVariable String detalleId,@Valid @RequestBody Detalle datos) { return detalles.actualizar(id,detalleId,datos); }
 @DeleteMapping("/{id}/detalles/{detalleId}") @Transactional public ResponseEntity<Void> eliminarDetalle(@PathVariable String id,@PathVariable String detalleId) { detalles.deHistoria(id,detalleId); detalles.eliminar(detalleId); return ResponseEntity.noContent().build(); }
}
