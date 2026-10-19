package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import ar.edu.clinica.api.Solicitudes.Persona;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
@Service public class MedicoService extends CrudTemplate<Medico,Persona> {
 public MedicoService(EntityManager em,MedicoFactory factory) { super(em,factory,Medico.class); }
 protected void validar(Persona d,String id) { unico("documento",d.documento(),id); }
 protected void asignar(Medico e,Persona d) { e.nombre=d.nombre().trim(); e.apellido=d.apellido().trim(); e.documento=d.documento().trim(); }
 protected void antesDeEliminar(Medico e) { sinReferencias("select count(r) from DetalleHistoriaClinica r where r.medico.id=:id and r.eliminado=false",e.id); }
}
