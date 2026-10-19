package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import ar.edu.clinica.api.Solicitudes.Historia;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
@Service public class HistoriaService extends CrudTemplate<HistoriaClinica,Historia> {
 private final PacienteService pacientes; private final UsuarioService usuarios;
 public HistoriaService(EntityManager em,HistoriaClinicaFactory f,PacienteService p,UsuarioService u) { super(em,f,HistoriaClinica.class); pacientes=p; usuarios=u; }
 protected void validar(Historia d,String id) { pacientes.buscar(d.pacienteId()); usuarios.buscar(d.usuarioId()); }
 protected void asignar(HistoriaClinica e,Historia d) { e.paciente=pacientes.buscar(d.pacienteId()); e.usuario=usuarios.buscar(d.usuarioId()); }
 protected void antesDeEliminar(HistoriaClinica e) { em.createQuery("update DetalleHistoriaClinica d set d.eliminado=true where d.historia.id=:id").setParameter("id",e.id).executeUpdate(); }
}
