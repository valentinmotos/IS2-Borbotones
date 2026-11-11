package com.dulcemayor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class PreventorService {
    private final PreventorRepository preventorRepository;
    private final UsuarioService usuarioService;
    public PreventorService(PreventorRepository preventorRepository, UsuarioService usuarioService) {
        this.preventorRepository = preventorRepository; this.usuarioService = usuarioService;
    }
    @Transactional
    public Preventor crearDemo(String correo) {
        Usuario usuario = usuarioService.buscarUsuarioPorNombre(correo);
        Preventor p = new Preventor(); p.setNombre("Pedro"); p.setApellido("Preventor");
        p.setFechaNacimiento(LocalDate.of(1990, 1, 1)); p.setTipoDocumento(TipoDocumento.DNI);
        p.setNumeroDocumento("30000001"); p.setLegajo("P-001"); p.setUsuario(usuario);
        return preventorRepository.saveAndFlush(p);
    }
    public Preventor buscarPorUsuario(String usuarioId) {
        return preventorRepository.findByUsuario_IdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene un preventor asociado."));
    }
    public Preventor buscarEntidad(String id) {
        return preventorRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el preventor."));
    }
}
