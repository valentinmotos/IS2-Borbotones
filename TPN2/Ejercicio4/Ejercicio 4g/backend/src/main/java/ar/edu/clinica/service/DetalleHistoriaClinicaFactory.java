package ar.edu.clinica.service;
import ar.edu.clinica.model.DetalleHistoriaClinica;
import org.springframework.stereotype.Component;
@Component public class DetalleHistoriaClinicaFactory extends RegistroFactory<DetalleHistoriaClinica> {
 protected DetalleHistoriaClinica instanciar() { return new DetalleHistoriaClinica(); }
}
