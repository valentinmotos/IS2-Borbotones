package ar.edu.clinica.service;
import ar.edu.clinica.model.Usuario;
import ar.edu.clinica.api.Solicitudes.Cuenta;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service public class UsuarioService extends CrudTemplate<Usuario,Cuenta> {
 private final PasswordEncoder encoder;
 public UsuarioService(EntityManager em,UsuarioFactory f,PasswordEncoder encoder) { super(em,f,Usuario.class); this.encoder=encoder; }
 protected void validar(Cuenta d,String id) {
  unico("nombreUsuario",d.nombreUsuario(),id);
  if(id==null && (d.clave()==null || d.clave().isBlank())) throw NegocioException.invalido("La clave es obligatoria al crear");
  if(d.clave()!=null && d.clave().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw NegocioException.invalido("La clave debe ocupar como máximo 72 bytes UTF-8 (límite de BCrypt)");
 }
 protected void asignar(Usuario e,Cuenta d) { e.nombreUsuario=d.nombreUsuario().trim(); if(d.clave()!=null) e.clave=encoder.encode(d.clave()); }
 protected void antesDeEliminar(Usuario e) { sinReferencias("select count(r) from HistoriaClinica r where r.usuario.id=:id and r.eliminado=false",e.id); }
}
