package ar.edu.clinica.service;
import ar.edu.clinica.model.Usuario;
import org.springframework.stereotype.Component;
@Component public class UsuarioFactory extends RegistroFactory<Usuario> {
 protected Usuario instanciar() { return new Usuario(); }
}
