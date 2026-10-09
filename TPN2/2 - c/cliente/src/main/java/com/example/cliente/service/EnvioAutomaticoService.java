package com.example.cliente.service;

import com.example.cliente.dao.EnvioAutomaticoDAO;
import com.example.cliente.dto.EnvioAutomaticoDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnvioAutomaticoService {
    private final EnvioAutomaticoDAO envioAutomaticoDAO;

    public EnvioAutomaticoService(EnvioAutomaticoDAO envioAutomaticoDAO) {
        this.envioAutomaticoDAO = envioAutomaticoDAO;
    }

    public List<EnvioAutomaticoDTO> listar() {
        return envioAutomaticoDAO.listar();
    }

    public EnvioAutomaticoDTO buscar(Long id) {
        return envioAutomaticoDAO.buscar(id);
    }

    public EnvioAutomaticoDTO nuevo() {
        EnvioAutomaticoDTO envio = new EnvioAutomaticoDTO();
        envio.setActivo(true);
        envio.setTipo("DEVOLUCION");
        return envio;
    }

    public void guardar(EnvioAutomaticoDTO envioAutomatico) {
        envioAutomaticoDAO.guardar(envioAutomatico);
    }

    public void eliminar(Long id) {
        envioAutomaticoDAO.eliminar(id);
    }
}
