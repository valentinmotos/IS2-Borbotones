package ar.edu.clinica.api;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class Solicitudes {
 private Solicitudes() {}
 public record Persona(@NotBlank @Size(max=100) String nombre,@NotBlank @Size(max=100) String apellido,@NotBlank @Size(max=30) String documento) {}
 public record Cuenta(@NotBlank @Size(max=80) String nombreUsuario,@Size(min=8,max=72) String clave) {
  @Override public String toString() { return "Cuenta[nombreUsuario="+nombreUsuario+", clave=***]"; }
 }
 public record Historia(@NotBlank String pacienteId,@NotBlank String usuarioId) {}
 public record Detalle(@NotNull @PastOrPresent LocalDate fechaHistoria,@NotBlank @Size(max=10000) String detalleHistoria,@NotBlank String medicoId,@NotBlank String tipo) {}
}
