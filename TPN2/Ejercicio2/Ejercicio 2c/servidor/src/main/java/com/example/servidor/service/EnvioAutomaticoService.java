package com.example.servidor.service;

import com.example.servidor.dao.EnvioAutomaticoDAO;
import com.example.servidor.model.EnvioAutomatico;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnvioAutomaticoService {
    public static final String TIPO_DEVOLUCION = "DEVOLUCION";
    public static final String TIPO_CUMPLEANOS = "CUMPLEANOS";

    private final EnvioAutomaticoDAO envioAutomaticoDAO;

    public EnvioAutomaticoService(EnvioAutomaticoDAO envioAutomaticoDAO) {
        this.envioAutomaticoDAO = envioAutomaticoDAO;
    }

    public List<EnvioAutomatico> listar() {
        return envioAutomaticoDAO.findAll();
    }

    public EnvioAutomatico buscar(Long id) {
        return envioAutomaticoDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Envio automatico no encontrado"));
    }

    public EnvioAutomatico guardar(EnvioAutomatico envioAutomatico) {
        return envioAutomaticoDAO.save(envioAutomatico);
    }

    public void eliminar(Long id) {
        envioAutomaticoDAO.deleteById(id);
    }

    public Optional<EnvioAutomatico> buscarActivoPorTipo(String tipo) {
        return envioAutomaticoDAO.findByTipo(tipo)
                .filter(envio -> Boolean.TRUE.equals(envio.getActivo()));
    }

    public boolean existeTipo(String tipo) {
        return envioAutomaticoDAO.existsByTipo(tipo);
    }
}
