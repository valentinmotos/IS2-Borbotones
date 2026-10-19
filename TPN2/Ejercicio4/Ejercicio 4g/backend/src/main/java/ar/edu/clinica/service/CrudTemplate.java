package ar.edu.clinica.service;
import ar.edu.clinica.model.Registro;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.List;
/** Template Method: fija la secuencia; las subclases implementan validación y asignación. */
@Transactional public abstract class CrudTemplate<T extends Registro,R> {
 protected final EntityManager em; protected final RegistroFactory<T> factory;
 private final Class<T> tipo;
 protected CrudTemplate(EntityManager em,RegistroFactory<T> factory,Class<T> tipo) { this.em=em; this.factory=factory; this.tipo=tipo; }
 public T crear(R datos) { return ejecutarAlta(datos); }
 public T modificar(String id,R datos) { return ejecutarModificacion(id,datos); }
 public void eliminar(String id) { ejecutarBaja(id); }
 // Los puntos de entrada públicos permiten la intercepción transaccional de Spring.
 protected final T ejecutarAlta(R datos) { validar(datos,null); T entidad=factory.crear(); asignar(entidad,datos); em.persist(entidad); return entidad; }
 protected final T ejecutarModificacion(String id,R datos) { T entidad=buscar(id); validar(datos,id); asignar(entidad,datos); return entidad; }
 protected final void ejecutarBaja(String id) { T entidad=buscar(id); antesDeEliminar(entidad); entidad.eliminado=true; }
 @Transactional(readOnly=true) public T buscar(String id) { T r=em.find(tipo,id); if(r==null || r.eliminado) throw new NegocioException(HttpStatus.NOT_FOUND,"Registro inexistente o eliminado"); return r; }
 @Transactional(readOnly=true) public List<T> listar() { return em.createQuery("from "+tipo.getSimpleName()+" r where r.eliminado=false order by r.id",tipo).getResultList(); }
 protected abstract void validar(R datos,String id);
 protected abstract void asignar(T entidad,R datos);
 protected void antesDeEliminar(T entidad) {}
 protected void unico(String campo,String valor,String id) {
  Long n=em.createQuery("select count(r) from "+tipo.getSimpleName()+" r where lower(r."+campo+")=lower(:valor) and (:id is null or r.id<>:id)",Long.class).setParameter("valor",valor.trim()).setParameter("id",id).getSingleResult();
  if(n>0) throw new NegocioException(HttpStatus.CONFLICT,"El "+campo+" ya está registrado (también se reservan las bajas)");
 }
 protected void sinReferencias(String jpql,String id) { if(em.createQuery(jpql,Long.class).setParameter("id",id).getSingleResult()>0) throw new NegocioException(HttpStatus.CONFLICT,"Tiene historias o atenciones activas asociadas"); }
}
