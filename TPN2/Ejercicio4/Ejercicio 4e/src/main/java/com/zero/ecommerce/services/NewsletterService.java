package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.NewsletterEnvioResultadoDTO;
import com.zero.ecommerce.dto.NewsletterEstadoDTO;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.NewsletterEnvioRepository;

@Service
public class NewsletterService {

    public static final int DIAS_ENTRE_ENVIOS = 10;
    public static final String ASUNTO = "Ofertas de Zero para vos";
    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private final CatalogoService catalogoService;
    private final ClienteService clienteService;
    private final EmailService emailService;
    private final NewsletterEnvioRepository envioRepository;
    private final String urlBase;

    public NewsletterService(CatalogoService catalogoService, ClienteService clienteService,
            EmailService emailService, NewsletterEnvioRepository envioRepository,
            @Value("${app.url-base:http://localhost:8080}") String urlBase) {
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
        this.emailService = emailService;
        this.envioRepository = envioRepository;
        this.urlBase = normalizarUrlBase(urlBase);
    }

    public NewsletterEstadoDTO obtenerEstado() {
        List<ProductoCatalogoDTO> ofertas = listarOfertas();
        int destinatarios = clienteService.listarClientesActivosConPerfilCompleto().size();
        LocalDate ultimo = envioRepository.buscarUltimoEnvio().orElse(null);
        LocalDate proximo = ultimo == null ? LocalDate.now() : ultimo.plusDays(DIAS_ENTRE_ENVIOS);
        return new NewsletterEstadoDTO(ofertas, destinatarios, ultimo, proximo);
    }

    /** Envía un correo individual a cada cliente para no exponer direcciones entre destinatarios. */
    public synchronized NewsletterEnvioResultadoDTO enviar() throws ErrorServiceException {
        List<ProductoCatalogoDTO> ofertas = listarOfertas();
        if (ofertas.isEmpty()) {
            log.info("Newsletter omitido: no hay productos en oferta con stock y precio vigente.");
            throw new ErrorServiceException("No hay ofertas con stock para enviar en el newsletter.");
        }

        List<Cliente> destinatarios = clienteService.listarClientesActivosConPerfilCompleto();
        if (destinatarios.isEmpty()) {
            log.info("Newsletter omitido: no hay clientes activos con perfil completo.");
            throw new ErrorServiceException("No hay clientes activos con perfil completo para recibirlo.");
        }

        for (Cliente cliente : destinatarios) {
            Map<String, Object> variables = variables(ofertas);
            variables.put("nombreCliente", cliente.getNombre());
            emailService.enviarSincronico(cliente.getUsuario().getNombreUsuario(), ASUNTO,
                    "newsletter", variables);
        }

        LocalDate fecha = LocalDate.now();
        envioRepository.guardarUltimoEnvio(fecha);
        log.info("Newsletter enviado correctamente a {} clientes.", destinatarios.size());
        return new NewsletterEnvioResultadoDTO(destinatarios.size(), fecha,
                fecha.plusDays(DIAS_ENTRE_ENVIOS));
    }

    public boolean correspondeEnviarHoy() {
        return envioRepository.buscarUltimoEnvio()
                .map(ultimo -> !LocalDate.now().isBefore(ultimo.plusDays(DIAS_ENTRE_ENVIOS)))
                .orElse(true);
    }

    /** HTML exacto del correo para el iframe de la pantalla administrativa. */
    public String renderizarVistaPrevia() throws ErrorServiceException {
        Map<String, Object> variables = variables(listarOfertas());
        variables.put("nombreCliente", "Cliente Zero");
        variables.put("logoUrl", urlBase + "/img/logo_zero_1.png");
        return emailService.renderizar(ASUNTO, "newsletter", variables);
    }

    private List<ProductoCatalogoDTO> listarOfertas() {
        return catalogoService.listar(CatalogoFiltro.ofertas());
    }

    private Map<String, Object> variables(List<ProductoCatalogoDTO> ofertas) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ofertas", ofertas);
        variables.put("urlBase", urlBase);
        return variables;
    }

    private String normalizarUrlBase(String valor) {
        String base = valor == null || valor.isBlank() ? "http://localhost:8080" : valor.strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
