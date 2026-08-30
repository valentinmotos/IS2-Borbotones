package com.example.mascotas.dto;

import com.example.mascotas.entidades.Foto;
import com.example.mascotas.entidades.Mascota;
import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.entidades.Zona;
import org.springframework.stereotype.Component;

@Component
public class DTOConversor {

    public FotoDTO convertir(Foto foto) {
        if (foto == null) {
            return null;
        }
        return new FotoDTO(foto.getId(), foto.getNombre(), foto.getMime(), foto.getContenido());
    }

    public ZonaDTO convertir(Zona zona) {
        if (zona == null) {
            return null;
        }
        return new ZonaDTO(zona.getId(), zona.getNombre(), zona.getDescripcion());
    }

    public UsuarioDTO convertir(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioDTO(usuario.getId(), usuario.getMail(), usuario.getNombre(),
                usuario.getApellido(), usuario.getAlta(), usuario.getBaja(),
                convertir(usuario.getZona()), convertir(usuario.getFoto()));
    }

    public MascotaDTO convertir(Mascota mascota) {
        if (mascota == null) {
            return null;
        }
        return new MascotaDTO(mascota.getId(), mascota.getNombre(), mascota.getSexo(),
                mascota.getTipo(), mascota.getAlta(), mascota.getBaja(),
                convertir(mascota.getUsuario()), convertir(mascota.getFoto()));
    }
}
