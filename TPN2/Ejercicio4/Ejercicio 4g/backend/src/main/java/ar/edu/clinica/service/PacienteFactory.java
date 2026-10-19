package ar.edu.clinica.service;
import ar.edu.clinica.model.Paciente;
import org.springframework.stereotype.Component;
@Component public class PacienteFactory extends RegistroFactory<Paciente> {
 protected Paciente instanciar() { return new Paciente(); }
}
