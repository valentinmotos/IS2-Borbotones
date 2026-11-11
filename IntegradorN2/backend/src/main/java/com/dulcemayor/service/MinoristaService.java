package com.dulcemayor.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.enums.TipoDocumento;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class MinoristaService {
    private final MinoristaRepository minoristaRepository;
    public MinoristaService(MinoristaRepository minoristaRepository) { this.minoristaRepository = minoristaRepository; }
    public List<MinoristaOpcion> listarActivos() {
        return minoristaRepository.findByEliminadoFalse().stream()
                .sorted(java.util.Comparator.comparing(com.dulcemayor.entity.Minorista::getApellido)
                        .thenComparing(com.dulcemayor.entity.Minorista::getNombre))
                .map(m -> new MinoristaOpcion(m.getId(), m.getNombre(), m.getApellido(), m.getNumeroDocumento())).toList();
    }
    public com.dulcemayor.entity.Minorista buscarEntidad(String id) {
        return minoristaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new com.dulcemayor.exception.RecursoNoEncontradoException("No se encontró el minorista."));
    }
    @Transactional
    public Minorista crearDemo() {
        Minorista m = new Minorista(); m.setNombre("Kiosco"); m.setApellido("Centro");
        m.setFechaNacimiento(LocalDate.of(1985, 1, 1)); m.setTipoDocumento(TipoDocumento.DNI);
        m.setNumeroDocumento("30000002");
        return minoristaRepository.save(m);
    }
}
