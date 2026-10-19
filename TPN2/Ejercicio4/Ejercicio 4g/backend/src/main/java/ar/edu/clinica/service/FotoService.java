package ar.edu.clinica.service;

import ar.edu.clinica.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Set;

@Service
@Transactional
public class FotoService {
    private final PacienteService pacientes;
    private final FotoPacienteFactory factory;
    private final EntityManager em;
    public FotoService(PacienteService pacientes, FotoPacienteFactory factory, EntityManager em) {
        this.pacientes=pacientes; this.factory=factory; this.em=em;
    }
    public FotoPaciente guardar(String pacienteId, MultipartFile archivo) throws IOException {
        Paciente p=pacientes.buscar(pacienteId);
        if (archivo.isEmpty() || archivo.getSize()>2*1024*1024) throw NegocioException.invalido("La foto debe pesar entre 1 byte y 2 MB");
        String mime=archivo.getContentType();
        if (!Set.of("image/jpeg","image/png").contains(mime == null ? "" : mime)) throw NegocioException.invalido("Solo se admiten fotos PNG o JPEG");
        byte[] contenido=archivo.getBytes();
        // Comprueba que el archivo es una imagen real, además del MIME declarado.
        try (var in=new java.io.ByteArrayInputStream(contenido); var imageIn=javax.imageio.ImageIO.createImageInputStream(in)) {
            var readers=javax.imageio.ImageIO.getImageReaders(imageIn);
            if (!readers.hasNext()) throw NegocioException.invalido("El contenido no es una imagen válida");
            var reader=readers.next();
            try {
                reader.setInput(imageIn);
                String formato=reader.getFormatName();
                boolean coincide=mime.equals("image/png") ? formato.equalsIgnoreCase("png") : formato.equalsIgnoreCase("jpeg");
                if (!coincide || (long)reader.getWidth(0)*reader.getHeight(0)>20_000_000) throw NegocioException.invalido("Imagen inválida o demasiado grande");
                if (reader.read(0)==null) throw NegocioException.invalido("Imagen inválida");
            } finally { reader.dispose(); }
        }
        if (p.foto!=null) p.foto.eliminado=true;
        FotoPaciente foto=factory.crear();
        foto.nombre=org.springframework.util.StringUtils.cleanPath(archivo.getOriginalFilename()==null?"foto":archivo.getOriginalFilename());
        foto.mime=mime; foto.contenido=contenido;
        em.persist(foto); p.foto=foto;
        return foto;
    }
    @Transactional(readOnly=true)
    public FotoPaciente buscar(String pacienteId) {
        FotoPaciente foto=pacientes.buscar(pacienteId).foto;
        if (foto==null || foto.eliminado) throw new NegocioException(org.springframework.http.HttpStatus.NOT_FOUND,"El paciente no tiene foto");
        return foto;
    }
    public void eliminar(String pacienteId) { Paciente p=pacientes.buscar(pacienteId); buscar(pacienteId).eliminado=true; p.foto=null; }
}
