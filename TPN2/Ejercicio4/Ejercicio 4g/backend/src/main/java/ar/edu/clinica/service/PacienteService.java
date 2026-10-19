package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import ar.edu.clinica.api.Solicitudes.Persona;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
@Service public class PacienteService extends CrudTemplate<Paciente,Persona> {
 public PacienteService(EntityManager em,PacienteFactory factory) { super(em,factory,Paciente.class); }
 protected void validar(Persona d,String id) { unico("documento",d.documento(),id); }
 protected void asignar(Paciente e,Persona d) { e.nombre=d.nombre().trim(); e.apellido=d.apellido().trim(); e.documento=d.documento().trim(); }
 protected void antesDeEliminar(Paciente e) { sinReferencias("select count(r) from HistoriaClinica r where r.paciente.id=:id and r.eliminado=false",e.id); }
}
