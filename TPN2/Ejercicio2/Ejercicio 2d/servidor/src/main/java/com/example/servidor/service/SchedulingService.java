package com.example.servidor.service;

import com.example.servidor.model.EnvioAutomatico;
import com.example.servidor.model.Persona;
import com.example.servidor.model.Prestamo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SchedulingService {
    private final EnvioAutomaticoService envioAutomaticoService;
    private final PrestamoService prestamoService;
    private final PersonaService personaService;
    private final EmailService emailService;

    public SchedulingService(EnvioAutomaticoService envioAutomaticoService,
                             PrestamoService prestamoService,
                             PersonaService personaService,
                             EmailService emailService) {
        this.envioAutomaticoService = envioAutomaticoService;
        this.prestamoService = prestamoService;
        this.personaService = personaService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.scheduling.cron:0 0 8 * * *}")
    public void ejecutarEnviosAutomaticos() {
        enviarRecordatoriosDevolucion();
        enviarSaludosCumpleanos();
    }

    private void enviarRecordatoriosDevolucion() {
        envioAutomaticoService.buscarActivoPorTipo(EnvioAutomaticoService.TIPO_DEVOLUCION)
                .ifPresent(envio -> prestamoService.listarConDevolucionManana()
                        .forEach(prestamo -> enviarRecordatorio(envio, prestamo)));
    }

    private void enviarRecordatorio(EnvioAutomatico envio, Prestamo prestamo) {
        Persona persona = prestamo.getPersona();
        if (persona == null) {
            return;
        }
        String cuerpo = envio.getCuerpoHtml()
                .replace("{nombre}", valor(persona.getNombre()))
                .replace("{libro}", prestamo.getLibro() != null ? valor(prestamo.getLibro().getTitulo()) : "")
                .replace("{fechaDevolucion}", prestamo.getFechaDevolucion() != null ? prestamo.getFechaDevolucion().toString() : "");
        emailService.enviarHtml(persona.getEmail(), envio.getAsunto(), cuerpo);
    }

    private void enviarSaludosCumpleanos() {
        envioAutomaticoService.buscarActivoPorTipo(EnvioAutomaticoService.TIPO_CUMPLEANOS)
                .ifPresent(envio -> personaService.listarConFechaNacimiento().stream()
                        .filter(this::cumpleHoy)
                        .forEach(persona -> enviarCumpleanos(envio, persona)));
    }

    private boolean cumpleHoy(Persona persona) {
        LocalDate hoy = LocalDate.now();
        return persona.getFechaNacimiento().getDayOfMonth() == hoy.getDayOfMonth()
                && persona.getFechaNacimiento().getMonth() == hoy.getMonth();
    }

    private void enviarCumpleanos(EnvioAutomatico envio, Persona persona) {
        String cuerpo = envio.getCuerpoHtml().replace("{nombre}", valor(persona.getNombre()));
        emailService.enviarHtml(persona.getEmail(), envio.getAsunto(), cuerpo);
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
