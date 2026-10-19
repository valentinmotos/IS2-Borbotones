package ar.edu.clinica.service;
import ar.edu.clinica.model.HistoriaClinica;
import org.springframework.stereotype.Component;
@Component public class HistoriaClinicaFactory extends RegistroFactory<HistoriaClinica> {
 protected HistoriaClinica instanciar() { return new HistoriaClinica(); }
}
