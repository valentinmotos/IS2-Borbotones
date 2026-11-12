package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dulcemayor.dto.EntregaRequest;
import com.dulcemayor.dto.EntregaResponse;
import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.dto.ParadaRutaResponse;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.dto.TomarPedidosRequest;
import com.dulcemayor.dto.mapper.ParadaRutaMapperImpl;
import com.dulcemayor.entity.BaseEntity;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.ParadaRuta;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Ruta;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.entity.enums.RutaEstado;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.ParadaRutaRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.RutaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** E5-02: tomar pedidos para reparto, entregar, proponer el siguiente punto y avisar al próximo comercio. */
@ExtendWith(MockitoExtension.class)
class DistribucionServiceTest {

    // Mismos comercios que RutaServiceTest: Centro, Godoy Cruz (≈4,2 km al sur) y Las Heras (≈6,5 km al norte).
    private static final double[] CENTRO = {-32.8878, -68.8421};
    private static final double[] GODOY_CRUZ = {-32.9260, -68.8440};
    private static final double[] LAS_HERAS = {-32.8300, -68.8200};
    private static final String USUARIO = "usr-dist";

    @Mock private RutaRepository rutaRepository;
    @Mock private ParadaRutaRepository paradaRepository;
    @Mock private PuntoDeVentaRepository puntoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PreventorRepository preventorRepository;
    @Mock private DistribuidorRepository distribuidorRepository;
    @Mock private PedidoService pedidoService;
    @Mock private PedidoAlmacenService pedidoAlmacen;
    @Mock private FacturaRepository facturaRepository;
    @Mock private HistorialEstadoPedidoRepository historialRepository;
    @Mock private ApplicationEventPublisher eventos;

    private DistribucionService service;
    private Distribuidor distribuidor;
    private final AtomicReference<Ruta> rutaDeHoy = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        RutaService rutas = new RutaService(rutaRepository, paradaRepository, puntoRepository, pedidoRepository,
                preventorRepository, distribuidorRepository, new PreventorService(preventorRepository),
                new ParadaRutaMapperImpl());
        service = new DistribucionService(new DistribuidorService(distribuidorRepository), rutas, pedidoService,
                pedidoAlmacen, pedidoRepository, facturaRepository, historialRepository, new ParadaRutaMapperImpl(),
                eventos);
        distribuidor = new Distribuidor();
        distribuidor.setId("dist-1");
        distribuidor.setNombre("Dante");
        distribuidor.setApellido("Distribuidor");

        lenient().when(distribuidorRepository.findParaRepartoByUsuario_IdAndEliminadoFalse(USUARIO))
                .thenReturn(Optional.of(distribuidor));
        lenient().when(rutaRepository.findFirstByDistribuidor_IdAndFechaAndEliminadoFalse(eq("dist-1"), any()))
                .thenAnswer(inv -> Optional.ofNullable(rutaDeHoy.get()));
        lenient().when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> {
            Ruta ruta = inv.getArgument(0);
            asignarId(ruta);
            rutaDeHoy.set(ruta);
            return ruta;
        });
        lenient().when(paradaRepository.save(any(ParadaRuta.class))).thenAnswer(inv -> {
            ParadaRuta parada = inv.getArgument(0);
            asignarId(parada);
            return parada;
        });
        // El State real lo prueba E4-01: acá el mock solo aplica la transición sobre la entidad.
        lenient().when(pedidoService.avanzar(anyString(), any(), eq(USUARIO), any(), eq("dist-1")))
                .thenAnswer(inv -> {
                    Pedido pedido = pedidoPorId(inv.getArgument(0));
                    EventoPedido evento = inv.getArgument(1);
                    EstadoPedido nuevo = pedido.getEstado().avanzar(evento);
                    ReflectionTestUtils.setField(pedido, "estado", nuevo);
                    return new HistorialEstadoPedidoResponse(nuevo, LocalDateTime.now(), USUARIO, "distribuidor",
                            evento.observacion(), new HistorialEstadoPedidoResponse.Transportista("dist-1", "Dante",
                                    "Distribuidor"));
                });
    }

    private final Map<String, Pedido> pedidos = new HashMap<>();

    @Test
    void tomarUnPedidoFacturadoYPagadoLoPasaAEnRepartoYLoSumaALaRutaDeHoy() {
        Pedido pedido = pedido("ped-centro", 1, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.FACTURADO);
        facturaDe(pedido, EstadoFactura.PAGADA);

        RutaResponse ruta = service.tomarPedidos(USUARIO, new TomarPedidosRequest(List.of("ped-centro")));

        verify(pedidoService).avanzar("ped-centro", EventoPedido.INICIAR_REPARTO, USUARIO, null, "dist-1");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
        assertThat(ruta.distribuidorId()).isEqualTo("dist-1");
        assertThat(ruta.paradas()).extracting(ParadaRutaResponse::pedidoId).containsExactly("ped-centro");
        assertThat(ruta.paradas().getFirst().visitada()).isFalse();
        assertThat(ruta.paradas().getFirst().estadoPedido()).isEqualTo(EstadoPedido.EN_REPARTO);
    }

    @Test
    void unPedidoSinPagarNoSePuedeTomar() {
        Pedido pedido = pedido("ped-centro", 7, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.FACTURADO);
        facturaDe(pedido, EstadoFactura.PENDIENTE_PAGO);

        assertThatThrownBy(() -> service.tomarPedidos(USUARIO, new TomarPedidosRequest(List.of("ped-centro"))))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("N° 7", "no está pagado");
        verify(pedidoService, never()).avanzar(anyString(), any(), anyString(), any(), any());
    }

    @Test
    void unPedidoQueNoEstaFacturadoNoSePuedeTomar() {
        pedido("ped-centro", 8, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.CARGADO);

        assertThatThrownBy(() -> service.tomarPedidos(USUARIO, new TomarPedidosRequest(List.of("ped-centro"))))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("no está listo para reparto");
        verify(pedidoService, never()).avanzar(anyString(), any(), anyString(), any(), any());
    }

    @Test
    void entregarRegistraLaEntregaProponeElSiguienteMasCercanoYAvisaAlProximo() {
        rutaConTresEntregas();

        EntregaResponse respuesta = service.entregar(USUARIO, "ped-centro", new EntregaRequest(null, null, "Recibió Ana"));

        verify(pedidoService).avanzar("ped-centro", EventoPedido.ENTREGAR, USUARIO, "Recibió Ana", "dist-1");
        assertThat(respuesta.historial().estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(respuesta.historial().transportista().id()).isEqualTo("dist-1");
        // Sin GPS se parte del comercio entregado: Godoy Cruz (≈4,2 km) queda antes que Las Heras (≈6,5 km).
        assertThat(respuesta.siguiente().pedidoId()).isEqualTo("ped-godoy");
        assertThat(respuesta.avisoEnCaminoA()).isEqualTo("Kiosco Godoy Cruz");
        assertThat(respuesta.ruta().siguiente().pedidoId()).isEqualTo("ped-godoy");
        assertThat(respuesta.ruta().paradas()).filteredOn(ParadaRutaResponse::visitada)
                .extracting(ParadaRutaResponse::pedidoId).containsExactly("ped-centro");

        ArgumentCaptor<PedidoEnCaminoEvent> evento = ArgumentCaptor.forClass(PedidoEnCaminoEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().pedidoId()).isEqualTo("ped-godoy");
        assertThat(evento.getValue().distribuidor()).isEqualTo("Dante Distribuidor");
    }

    @Test
    void conPosicionElSiguienteSeCalculaDesdeElDistribuidor() {
        rutaConTresEntregas();

        // Parado en Las Heras después de entregar en el centro: el más cercano es Las Heras.
        EntregaResponse respuesta = service.entregar(USUARIO, "ped-centro",
                new EntregaRequest(LAS_HERAS[0], LAS_HERAS[1], null));

        assertThat(respuesta.siguiente().pedidoId()).isEqualTo("ped-heras");
        assertThat(respuesta.ruta().distanciaKm()).isZero();
    }

    @Test
    void alMismoComercioNoSeLoAvisaDosVeces() {
        rutaConTresEntregas();

        service.entregar(USUARIO, "ped-centro", null);           // propone Godoy Cruz y lo avisa
        EntregaResponse segunda = service.entregar(USUARIO, "ped-heras", null); // vuelve a proponer Godoy Cruz

        assertThat(segunda.siguiente().pedidoId()).isEqualTo("ped-godoy");
        assertThat(segunda.avisoEnCaminoA()).isNull();
        verify(eventos, times(1)).publishEvent(any(PedidoEnCaminoEvent.class));
    }

    @Test
    void conLaUltimaEntregaLaRutaQuedaRealizadaYNoSeAvisaANadie() {
        Pedido pedido = pedido("ped-centro", 1, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.EN_REPARTO);
        enRuta(pedido);

        EntregaResponse respuesta = service.entregar(USUARIO, "ped-centro", null);

        assertThat(respuesta.siguiente()).isNull();
        assertThat(respuesta.avisoEnCaminoA()).isNull();
        assertThat(respuesta.ruta().estado()).isEqualTo(RutaEstado.REALIZADA);
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void unPedidoQueNoEstaEnSuRutaDevuelve404() {
        rutaConTresEntregas();

        assertThatThrownBy(() -> service.entregar(USUARIO, "pedido-de-otro", null))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("tu ruta");
        verify(pedidoService, never()).avanzar(anyString(), any(), anyString(), any(), any());
    }

    @Test
    void unaPosicionInvalidaSeRechazaSinNotificar() {
        rutaConTresEntregas();

        assertThatThrownBy(() -> service.entregar(USUARIO, "ped-centro", new EntregaRequest(200.0, 0.0, null)))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("posición");
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void losPedidosSinEntregarDeAyerPasanALaRutaDeHoy() {
        Pedido pendiente = pedido("ped-centro", 1, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.EN_REPARTO);
        Ruta ayer = new Ruta();
        asignarId(ayer);
        ayer.setFecha(LocalDate.now().minusDays(1));
        ayer.setEstado(RutaEstado.PENDIENTE);
        ayer.setDistribuidor(distribuidor);
        ParadaRuta parada = new ParadaRuta();
        asignarId(parada);
        parada.setPuntoDeVenta(pendiente.getPuntoDeVenta());
        parada.setPedido(pendiente);
        parada.setOrden(1);
        ayer.agregarParada(parada);
        when(rutaRepository.findByDistribuidor_IdAndFechaBeforeAndEstadoAndEliminadoFalse(
                eq("dist-1"), any(), eq(RutaEstado.PENDIENTE))).thenReturn(List.of(ayer));

        RutaResponse hoy = service.obtenerRutaDelDia(USUARIO, null, null);

        assertThat(hoy.fecha()).isEqualTo(LocalDate.now());
        assertThat(hoy.paradas()).extracting(ParadaRutaResponse::pedidoId).containsExactly("ped-centro");
        assertThat(ayer.getEstado()).isEqualTo(RutaEstado.INCOMPLETA);
    }

    @Test
    void sinPerfilDeDistribuidorSeRechaza() {
        when(distribuidorRepository.findParaRepartoByUsuario_IdAndEliminadoFalse("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerRutaDelDia("otro", null, null))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("perfil de distribuidor");
        verify(rutaRepository, never()).save(any(Ruta.class));
    }

    // ---------------------------------------------------------------- auxiliares

    private void rutaConTresEntregas() {
        enRuta(pedido("ped-centro", 1, punto("pv-centro", "Kiosco Centro", CENTRO), EstadoPedido.EN_REPARTO));
        enRuta(pedido("ped-godoy", 2, punto("pv-godoy", "Kiosco Godoy Cruz", GODOY_CRUZ), EstadoPedido.EN_REPARTO));
        enRuta(pedido("ped-heras", 3, punto("pv-heras", "Kiosco Las Heras", LAS_HERAS), EstadoPedido.EN_REPARTO));
    }

    /** Agrega el pedido a la ruta de hoy (la crea si hace falta), como si ya lo hubiera tomado. */
    private void enRuta(Pedido pedido) {
        Ruta ruta = rutaDeHoy.get();
        if (ruta == null) {
            ruta = new Ruta();
            asignarId(ruta);
            ruta.setFecha(LocalDate.now());
            ruta.setEstado(RutaEstado.PENDIENTE);
            ruta.setDistribuidor(distribuidor);
            rutaDeHoy.set(ruta);
        }
        ParadaRuta parada = new ParadaRuta();
        asignarId(parada);
        parada.setPuntoDeVenta(pedido.getPuntoDeVenta());
        parada.setPedido(pedido);
        parada.setOrden(ruta.getParadas().size() + 1);
        ruta.agregarParada(parada);
    }

    private Pedido pedido(String id, long numero, PuntoDeVenta punto, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setNumeroPedido(numero);
        pedido.setPuntoDeVenta(punto);
        ReflectionTestUtils.setField(pedido, "estado", estado);
        pedidos.put(id, pedido);
        lenient().when(pedidoRepository.buscarActivoParaActualizar(id)).thenReturn(Optional.of(pedido));
        return pedido;
    }

    private Pedido pedidoPorId(String id) {
        return pedidos.get(id);
    }

    private void facturaDe(Pedido pedido, EstadoFactura estado) {
        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setEstado(estado);
        when(facturaRepository.findByPedido_IdAndEliminadoFalse(pedido.getId())).thenReturn(Optional.of(factura));
    }

    private static PuntoDeVenta punto(String id, String nombre, double[] coordenadas) {
        Direccion direccion = new Direccion();
        direccion.setCalle("San Martín");
        direccion.setNumeracion("100");
        direccion.setLatitud(coordenadas[0]);
        direccion.setLongitud(coordenadas[1]);
        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setId(id);
        punto.setNombreComercio(nombre);
        punto.setDireccion(direccion);
        punto.setMinorista(new Minorista());
        return punto;
    }

    private static void asignarId(BaseEntity entidad) {
        if (entidad.getId() == null) {
            entidad.setId(UUID.randomUUID().toString());
        }
    }
}
