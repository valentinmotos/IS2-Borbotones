package com.dulcemayor.service;

import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.utils.TextoUtils;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NewsletterService {
    private static final String ASUNTO = "Ofertas del mes · Dulce Mayor";

    private final CatalogoService catalogo;
    private final MinoristaRepository minoristas;
    private final EmailService correo;
    private final String urlCatalogo;

    public NewsletterService(CatalogoService catalogo, MinoristaRepository minoristas, EmailService correo,
            @Value("${APP_URL_PUBLIC:http://localhost:5174}") String urlPublica) {
        this.catalogo = catalogo;
        this.minoristas = minoristas;
        this.correo = correo;
        String base = urlPublica.isBlank() ? "http://localhost:5174" : urlPublica.strip();
        this.urlCatalogo = base.replaceFirst("/+$", "") + "/catalogo";
    }

    public ResultadoEnvio enviarOfertas() {
        List<ProductoCatalogoDTO> ofertas = catalogo.listarOfertasDisponibles();
        if (ofertas.isEmpty()) {
            return new ResultadoEnvio(0, 0);
        }

        List<String> destinatarios = minoristas.listarCorreosActivos(RolUsuario.MINORISTA).stream()
                .filter(TextoUtils::esCorreoValido)
                .toList();
        Map<String, Object> variables = Map.of("ofertas", ofertas, "urlCatalogo", urlCatalogo);
        for (String destinatario : destinatarios) {
            correo.enviar(destinatario, ASUNTO, "correo/newsletter-ofertas", variables);
        }
        return new ResultadoEnvio(ofertas.size(), destinatarios.size());
    }

    public record ResultadoEnvio(int ofertas, int destinatariosProgramados) {
    }
}
