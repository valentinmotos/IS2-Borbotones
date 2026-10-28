package com.example.mascotas.servicios;

import com.example.mascotas.dto.Datos.*;
import com.example.mascotas.entidades.*;
import com.example.mascotas.repositorios.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.example.mascotas.servicios.UsuarioServicio.*;

@Service
@Transactional
public class MascotaServicio {
    private final MascotaRepositorio mascotas;
    private final VotoRepositorio votos;
    private final UsuarioServicio usuarios;
    public MascotaServicio(MascotaRepositorio mascotas, VotoRepositorio votos, UsuarioServicio usuarios) {
        this.mascotas=mascotas; this.votos=votos; this.usuarios=usuarios;
    }
    public List<MascotaVista> propias(String usuario) {
        return mascotas.findByUsuarioIdAndBajaIsNull(usuario).stream().map(MascotaVista::de).toList();
    }
    public MascotaVista crear(String usuario, MascotaEntrada entrada) {
        Mascota m=new Mascota();
        m.setUsuario(usuarios.actual(usuario)); m.setAlta(new Date());
        aplicar(m, entrada);
        return MascotaVista.de(mascotas.save(m));
    }
    public MascotaVista editar(String usuario, String id, MascotaEntrada entrada) {
        Mascota m=propia(usuario,id); aplicar(m,entrada); return MascotaVista.de(m);
    }
    private void aplicar(Mascota m, MascotaEntrada e) {
        m.setNombre(texto(e.nombre())); m.setSexo(e.sexo()); m.setTipo(e.tipo());
    }
    public void baja(String usuario, String id) { propia(usuario,id).setBaja(new Date()); }
    public Mascota buscar(String id) {
        return mascotas.findById(id).filter(m -> m.getBaja()==null && m.getUsuario().getBaja()==null)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Mascota no encontrada"));
    }
    public Mascota propia(String usuario, String id) {
        Mascota m=buscar(id);
        if (!m.getUsuario().getId().equals(usuario)) throw error(HttpStatus.FORBIDDEN,"La mascota pertenece a otro usuario");
        return m;
    }
    public List<MascotaVista> explorar(String usuario, String origen) {
        Mascota propia=propia(usuario,origen);
        Set<String> votadas=new HashSet<>();
        votos.delUsuario(usuario).stream().filter(v -> v.getMascota1().getId().equals(origen))
                .forEach(v -> votadas.add(v.getMascota2().getId()));
        return mascotas.findByBajaIsNullAndUsuarioBajaIsNullAndUsuarioIdNot(usuario).stream()
                .filter(m -> compatibles(propia,m) && !votadas.contains(m.getId()))
                .map(MascotaVista::de).toList();
    }
    private boolean compatibles(Mascota a, Mascota b) {
        return a.getTipo()==b.getTipo() && a.getSexo()!=b.getSexo()
                && a.getUsuario().getZona().getId().equals(b.getUsuario().getZona().getId());
    }
    public VotoVista votar(String usuario, VotoEntrada e) {
        Mascota origen=propia(usuario,e.idMascotaPropia()), destino=buscar(e.idMascotaVotada());
        if (destino.getUsuario().getId().equals(usuario)) throw error(HttpStatus.BAD_REQUEST,"No puede votar sus propias mascotas");
        if (!compatibles(origen,destino)) throw error(HttpStatus.BAD_REQUEST,"Las mascotas deben ser de la misma especie y zona, y de distinto sexo");
        if (votos.findByMascota1IdAndMascota2Id(origen.getId(),destino.getId()).isPresent())
            throw error(HttpStatus.CONFLICT,"Ya voto esta mascota");
        Voto v=new Voto(); v.setMascota1(origen); v.setMascota2(destino); v.setFecha(new Date());
        votos.findByMascota1IdAndMascota2Id(destino.getId(),origen.getId()).ifPresent(inverso -> {
            Date ahora=new Date(); inverso.setRespuesta(ahora); v.setRespuesta(ahora);
        });
        return VotoVista.de(votos.saveAndFlush(v),usuario);
    }
    public List<VotoVista> listarVotos(String usuario) {
        return votos.delUsuario(usuario).stream()
                .filter(v -> v.getMascota1().getBaja()==null && v.getMascota2().getBaja()==null
                        && v.getMascota1().getUsuario().getBaja()==null && v.getMascota2().getUsuario().getBaja()==null)
                .map(v -> VotoVista.de(v,usuario)).toList();
    }
    public VotoVista responder(String usuario, String id) {
        Voto v=votos.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Voto no encontrado"));
        propia(usuario,v.getMascota2().getId()); buscar(v.getMascota1().getId());
        if (v.getRespuesta()!=null) throw error(HttpStatus.CONFLICT,"El voto ya tiene match");
        v.setRespuesta(new Date());
        return VotoVista.de(v,usuario);
    }
}
