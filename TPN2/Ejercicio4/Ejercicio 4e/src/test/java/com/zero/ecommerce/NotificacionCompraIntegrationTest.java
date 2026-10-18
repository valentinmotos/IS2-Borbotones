package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.repositories.ConfiguracionCorreoEmpresaRepository;
import com.zero.ecommerce.services.ConfiguracionCorreoEmpresaService;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.NotificacionCompraService;
import com.zero.ecommerce.services.OrdenCompraService;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * Correos de la compra (E4-05) con los pedidos de demostración del seeder y GreenMail. Como EmailAsyncIntegrationTest,
 * no es @Transactional: el envío corre en otro hilo y tiene que leer la configuración ya guardada.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
class NotificacionCompraIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE));

    private final NotificacionCompraService notificacionService;
    private final OrdenCompraService ordenCompraService;
    private final ConfiguracionCorreoEmpresaService configuracionService;
    private final ConfiguracionCorreoEmpresaRepository configuracionRepository;
    private final EmpresaService empresaService;
    private final TransactionTemplate transaccion;

    NotificacionCompraIntegrationTest(@Autowired NotificacionCompraService notificacionService,
            @Autowired OrdenCompraService ordenCompraService,
            @Autowired ConfiguracionCorreoEmpresaService configuracionService,
            @Autowired ConfiguracionCorreoEmpresaRepository configuracionRepository,
            @Autowired EmpresaService empresaService, @Autowired TransactionTemplate transaccion) {
        this.notificacionService = notificacionService;
        this.ordenCompraService = ordenCompraService;
        this.configuracionService = configuracionService;
        this.configuracionRepository = configuracionRepository;
        this.empresaService = empresaService;
        this.transaccion = transaccion;
    }

    @BeforeEach
    void configurarCorreo() throws Exception {
        configuracionService.crearConfiguracionCorreoEmpresa(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE,
                String.valueOf(ServerSetupTest.SMTP.getPort()), "127.0.0.1", false,
                empresaService.buscarSedeCentral().getId());
    }

    @AfterEach
    void borrarConfiguracion() {
        configuracionRepository.deleteAll();
    }

    @Test
    void laConfirmacionLlegaConTodosLosDatosDeLaCompra() throws Exception {
        // ORD-DEMO0001: Lucía Gómez, pendiente de pago por transferencia, factura N.º 3.
        String idOrden = enTransaccion("ORD-DEMO0001", true);

        MimeMessage recibido = esperarCorreo();
        String html = html(recibido);
        assertThat(recibido.getSubject()).isEqualTo(NotificacionCompraService.ASUNTO_CONFIRMACION);
        assertThat(recibido.getAllRecipients()[0].toString()).isEqualTo("lucia.gomez@mail.com");
        assertThat(html).contains("ORD-DEMO0001", "Transferencia", "N.º 3", "Cómo pagar", "Gorra",
                "http://localhost:8080/cliente/compras/" + idOrden)
                .doesNotContain("Pagar con Mercado Pago");
    }

    @Test
    void elCambioDeEstadoAvisaConLinkAlSeguimiento() throws Exception {
        // ORD-DEMO0002: Martín Pérez, pendiente de envío.
        String idOrden = enTransaccion("ORD-DEMO0002", false);

        MimeMessage recibido = esperarCorreo();
        String html = html(recibido);
        assertThat(recibido.getSubject()).isEqualTo(NotificacionCompraService.ASUNTO_CAMBIO_ESTADO);
        assertThat(recibido.getAllRecipients()[0].toString()).isEqualTo("martin.perez@mail.com");
        assertThat(html).contains("ORD-DEMO0002", "Pendiente de envío",
                "http://localhost:8080/cliente/compras/" + idOrden);
    }

    // Como en el checkout y el panel, el aviso se pide dentro de la transacción que tiene la orden cargada.
    private String enTransaccion(String identificador, boolean confirmacion) {
        return transaccion.execute(estado -> {
            OrdenCompra orden = ordenCompraService.listarPedidoActivo().stream()
                    .filter(o -> identificador.equals(o.getIdentificadorCompra()))
                    .findFirst().orElseThrow();
            if (confirmacion) {
                notificacionService.enviarConfirmacion(orden);
            } else {
                notificacionService.notificarCambioEstado(orden);
            }
            return orden.getId();
        });
    }

    private MimeMessage esperarCorreo() {
        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1)).isTrue();
        return GREEN_MAIL.getReceivedMessages()[0];
    }

    // El HTML del correo ya decodificado (el cuerpo crudo viene en quoted-printable).
    private String html(Part parte) throws Exception {
        if (parte.isMimeType("text/html")) {
            return (String) parte.getContent();
        }
        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String encontrado = html(multipart.getBodyPart(i));
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }
}
