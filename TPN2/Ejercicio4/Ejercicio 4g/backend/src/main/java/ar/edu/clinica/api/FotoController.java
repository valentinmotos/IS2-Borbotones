package ar.edu.clinica.api;

import ar.edu.clinica.service.FotoService;
import ar.edu.clinica.model.FotoPaciente;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.io.IOException;
@RestController @RequestMapping("/api/pacientes/{id}/foto")
public class FotoController {
 private final FotoService fotos;
 public FotoController(FotoService fotos) { this.fotos=fotos; }
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<FotoPaciente> guardar(@PathVariable String id,@RequestPart MultipartFile archivo) throws IOException { return ResponseEntity.status(201).body(fotos.guardar(id,archivo)); }
 @GetMapping public ResponseEntity<byte[]> ver(@PathVariable String id) { var f=fotos.buscar(id); return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.mime)).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(f.contenido); }
 @DeleteMapping public ResponseEntity<Void> eliminar(@PathVariable String id) { fotos.eliminar(id); return ResponseEntity.noContent().build(); }
}
