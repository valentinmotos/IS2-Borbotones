package com.dulcemayor.service;

import com.dulcemayor.dto.FacturaResponse;
import com.dulcemayor.dto.mapper.FacturaMapper;
import com.dulcemayor.entity.ContactoCorreoElectronico;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.repository.ContactoCorreoElectronicoRepository;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.utils.TextoUtils;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FacturaCorreoService {
    private static final Logger log = LoggerFactory.getLogger(FacturaCorreoService.class);

    private final FacturaRepository facturas;
    private final ContactoCorreoElectronicoRepository contactos;
    private final FacturaMapper mapper;
    private final FacturaPdfService pdf;
    private final EmailService correo;

    public FacturaCorreoService(FacturaRepository facturas, ContactoCorreoElectronicoRepository contactos,
            FacturaMapper mapper, FacturaPdfService pdf, EmailService correo) {
        this.facturas = facturas;
        this.contactos = contactos;
        this.mapper = mapper;
        this.pdf = pdf;
        this.correo = correo;
    }

    @Async
    @TransactionalEventListener
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void alFacturar(FacturaEmitidaEvent evento) {
        Factura factura = facturas.findByIdAndEliminadoFalse(evento.facturaId()).orElse(null);
        if (factura == null) {
            log.warn("No se encontró la factura {} para preparar su correo.", evento.facturaId());
            return;
        }

        Minorista minorista = factura.getPedido().getPuntoDeVenta().getMinorista();
        Optional<String> destino = buscarDestino(minorista);
        if (destino.isEmpty()) {
            log.warn("No hay correo para el responsable del pedido {}.", evento.pedidoId());
            return;
        }

        FacturaResponse resumen = mapper.aRespuesta(factura);
        var adjunto = new EmailService.Adjunto(pdf.nombreArchivo(resumen.id()),
                pdf.generarFacturaPdf(resumen.id()), MediaType.APPLICATION_PDF_VALUE);
        correo.enviar(destino.get(), "Factura N° " + resumen.numeroVisible() + " · Dulce Mayor",
                "correo/factura-emitida", Map.of("factura", resumen), adjunto);
    }

    private Optional<String> buscarDestino(Minorista minorista) {
        return contactos.findByMinorista_IdAndEliminadoFalseOrderByIdAsc(minorista.getId()).stream()
                .map(ContactoCorreoElectronico::getEmail)
                .filter(TextoUtils::esCorreoValido)
                .findFirst()
                .or(() -> Optional.ofNullable(minorista.getUsuario())
                        .filter(usuario -> !usuario.isEliminado())
                        .map(Usuario::getNombreUsuario)
                        .filter(TextoUtils::esCorreoValido))
                .map(String::strip);
    }
}
