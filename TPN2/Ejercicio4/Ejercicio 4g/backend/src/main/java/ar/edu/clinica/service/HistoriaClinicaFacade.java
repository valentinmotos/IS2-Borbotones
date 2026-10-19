package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
/** Facade estructural: compone un expediente sin exponer la coordinación al controlador. */
@Service public class HistoriaClinicaFacade {
 private final HistoriaService historias; private final DetalleService detalles;
 public HistoriaClinicaFacade(HistoriaService h,DetalleService d) { historias=h; detalles=d; }
 public record Expediente(HistoriaClinica historia,List<DetalleHistoriaClinica> atenciones,int totalAtenciones) {}
 @Transactional(readOnly=true) public Expediente consultar(String id) { HistoriaClinica h=historias.buscar(id); List<DetalleHistoriaClinica> d=detalles.deHistoria(id); return new Expediente(h,d,d.size()); }
}
