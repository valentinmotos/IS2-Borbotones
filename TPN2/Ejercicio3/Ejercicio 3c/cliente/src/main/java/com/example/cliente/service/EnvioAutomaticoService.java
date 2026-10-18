package com.example.cliente.service;

import com.example.cliente.dao.EnvioAutomaticoDAO;
import com.example.cliente.dto.EnvioAutomaticoDTO;
import org.springframework.stereotype.Service;

@Service
public class EnvioAutomaticoService extends BaseService<EnvioAutomaticoDTO> {
    public EnvioAutomaticoService(EnvioAutomaticoDAO envioAutomaticoDAO) {
        super(envioAutomaticoDAO);
    }

    @Override
    protected EnvioAutomaticoDTO crearDTO() {
        return new EnvioAutomaticoDTO();
    }

    @Override
    protected void asignarValoresPorDefecto(EnvioAutomaticoDTO envio) {
        envio.setActivo(true);
        envio.setTipo("DEVOLUCION");
    }
}
