package com.example.servidor.service;

import com.example.servidor.dao.EnvioAutomaticoDAO;
import com.example.servidor.model.EnvioAutomatico;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EnvioAutomaticoService extends BaseService<EnvioAutomatico> {
    public static final String TIPO_DEVOLUCION = "DEVOLUCION";
    public static final String TIPO_CUMPLEANOS = "CUMPLEANOS";

    private final EnvioAutomaticoDAO envioAutomaticoDAO;

    public EnvioAutomaticoService(EnvioAutomaticoDAO envioAutomaticoDAO) {
        super(envioAutomaticoDAO, "Envio automatico no encontrado");
        this.envioAutomaticoDAO = envioAutomaticoDAO;
    }

    public Optional<EnvioAutomatico> buscarActivoPorTipo(String tipo) {
        return envioAutomaticoDAO.findByTipo(tipo)
                .filter(envio -> Boolean.TRUE.equals(envio.getActivo()));
    }

    public boolean existeTipo(String tipo) {
        return envioAutomaticoDAO.existsByTipo(tipo);
    }
}
