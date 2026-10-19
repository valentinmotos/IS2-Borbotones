package ar.edu.clinica.service;
import ar.edu.clinica.model.FotoPaciente;
import org.springframework.stereotype.Component;
@Component public class FotoPacienteFactory extends RegistroFactory<FotoPaciente> {
 protected FotoPaciente instanciar() { return new FotoPaciente(); }
}
