package ar.edu.clinica.config;

import ar.edu.clinica.api.Solicitudes.*;
import ar.edu.clinica.service.*;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name="clinica.demo",havingValue="true")
public class DatosDemo implements CommandLineRunner {
    private final EntityManager em;
    private final UsuarioService usuarios;
    private final PacienteService pacientes;
    private final MedicoService medicos;
    private final HistoriaService historias;
    private final DetalleService detalles;
    public DatosDemo(EntityManager em,UsuarioService u,PacienteService p,MedicoService m,HistoriaService h,DetalleService d) {
        this.em=em; usuarios=u; pacientes=p; medicos=m; historias=h; detalles=d;
    }
    @Override @Transactional public void run(String... args) {
        if (em.createQuery("select count(u) from Usuario u",Long.class).getSingleResult()>0) return;
        var u=usuarios.crear(new Cuenta("demo","Consultorio123!"));
        var p=pacientes.crear(new Persona("Lucía","Gómez","30111222"));
        var m=medicos.crear(new Persona("Martín","Pérez","25333444"));
        var h=historias.crear(new Historia(p.id,u.id));
        detalles.agregar(h.id,new Detalle(LocalDate.now(),"Consulta inicial. Se registra evolución favorable y seguimiento en 30 días.",m.id,"GENERAL"));
    }
}
