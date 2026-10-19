package ar.edu.clinica.service;
import ar.edu.clinica.api.Solicitudes.Detalle;
import org.springframework.stereotype.Component;
@Component public class ConsultaGeneral implements PoliticaAtencion {
 public String tipo() { return "GENERAL"; }
 public void validar(Detalle d) { if(d.detalleHistoria().trim().length()<10) throw NegocioException.invalido("Describa la consulta con al menos 10 caracteres"); }
}
