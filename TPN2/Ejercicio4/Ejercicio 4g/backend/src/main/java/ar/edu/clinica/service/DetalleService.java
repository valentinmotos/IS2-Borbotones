package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import ar.edu.clinica.api.Solicitudes.Detalle;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class DetalleService extends CrudTemplate<DetalleHistoriaClinica,DetalleService.DatosAtencion> {
 public record DatosAtencion(String historiaId,Detalle detalle) {}
 private final MedicoService medicos; private final HistoriaService historias; private final List<PoliticaAtencion> politicas;
 public DetalleService(EntityManager em,DetalleHistoriaClinicaFactory f,MedicoService m,HistoriaService h,List<PoliticaAtencion> p) { super(em,f,DetalleHistoriaClinica.class); medicos=m; historias=h; politicas=p; }
 protected void validar(DatosAtencion datos,String id) { var d=datos.detalle(); historias.buscar(datos.historiaId()); medicos.buscar(d.medicoId()); politicas.stream().filter(p->p.tipo().equals(d.tipo())).findFirst().orElseThrow(()->NegocioException.invalido("Tipo de atención desconocido")).validar(d); }
 protected void asignar(DetalleHistoriaClinica e,DatosAtencion datos) { var d=datos.detalle(); e.historia=historias.buscar(datos.historiaId()); e.fechaHistoria=d.fechaHistoria(); e.detalleHistoria=d.detalleHistoria().trim(); e.medico=medicos.buscar(d.medicoId()); }
 @Transactional public DetalleHistoriaClinica agregar(String historiaId,Detalle d) { return crear(new DatosAtencion(historiaId,d)); }
 @Transactional public DetalleHistoriaClinica actualizar(String historiaId,String id,Detalle d) { deHistoria(historiaId,id); return modificar(id,new DatosAtencion(historiaId,d)); }
 public DetalleHistoriaClinica deHistoria(String historiaId,String id) { historias.buscar(historiaId); DetalleHistoriaClinica d=buscar(id); if(!d.historia.id.equals(historiaId)) throw new NegocioException(org.springframework.http.HttpStatus.NOT_FOUND,"La atención no pertenece a esta historia"); return d; }
 @Transactional(readOnly=true) public List<DetalleHistoriaClinica> deHistoria(String id) { historias.buscar(id); return em.createQuery("from DetalleHistoriaClinica d where d.historia.id=:id and d.eliminado=false order by d.fechaHistoria desc,d.id",DetalleHistoriaClinica.class).setParameter("id",id).getResultList(); }
}
