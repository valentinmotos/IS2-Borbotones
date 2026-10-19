package ar.edu.clinica.service;
import ar.edu.clinica.model.Medico;
import org.springframework.stereotype.Component;
@Component public class MedicoFactory extends RegistroFactory<Medico> {
 protected Medico instanciar() { return new Medico(); }
}
