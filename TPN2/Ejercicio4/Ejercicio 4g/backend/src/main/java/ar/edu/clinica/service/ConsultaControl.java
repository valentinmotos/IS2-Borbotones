package ar.edu.clinica.service;
import ar.edu.clinica.api.Solicitudes.Detalle;
import org.springframework.stereotype.Component;
@Component public class ConsultaControl implements PoliticaAtencion {
 public String tipo() { return "CONTROL"; }
 public void validar(Detalle d) { if(d.detalleHistoria().trim().length()<20) throw NegocioException.invalido("El control requiere al menos 20 caracteres de evolución"); }
}
