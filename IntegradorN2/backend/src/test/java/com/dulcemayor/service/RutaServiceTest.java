package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.dulcemayor.dto.ParadaRutaResponse;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.dto.mapper.ParadaRutaMapperImpl;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.ParadaRuta;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Ruta;
import com.dulcemayor.entity.enums.RutaEstado;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.ParadaRutaRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.RutaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** E5-01: ruta del día del preventor y próximo punto más cercano. */
@ExtendWith(MockitoExtension.class)
class RutaServiceTest {

    // Tres comercios de Mendoza: Centro, Godoy Cruz (≈4,2 km al sur) y Las Heras (≈7 km al norte).
    private static final double[] CENTRO = {-32.8878, -68.8421};
    private static final double[] GODOY_CRUZ = {-32.9260, -68.8440};
    private static final double[] LAS_HERAS = {-32.8300, -68.8200};

    @Mock private RutaRepository rutaRepository;
    @Mock private ParadaRutaRepository paradaRepository;
    @Mock private PuntoDeVentaRepository puntoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PreventorRepository preventorRepository;
    @Mock private DistribuidorRepository distribuidorRepository;

    private RutaService service;
    private Preventor preventor;
    private PuntoDeVenta centro;
    private PuntoDeVenta godoyCruz;
    private PuntoDeVenta lasHeras;
    private final AtomicReference<Ruta> rutaGuardada = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new RutaService(rutaRepository, paradaRepository, puntoRepository, pedidoRepository,
                preventorRepository, distribuidorRepository, new PreventorService(preventorRepository),
                new ParadaRutaMapperImpl());
        preventor = new Preventor();
        preventor.setId("prev-1");
        centro = punto("pv-centro", "Kiosco Centro", CENTRO);
        godoyCruz = punto("pv-godoy", "Kiosco Godoy Cruz", GODOY_CRUZ);
        lasHeras = punto("pv-heras", "Kiosco Las Heras", LAS_HERAS);

        lenient().when(preventorRepository.findParaCargaByUsuario_IdAndEliminadoFalse("usr-prev"))
                .thenReturn(Optional.of(preventor));
        lenient().when(puntoRepository.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc("prev-1"))
                .thenReturn(List.of(centro, godoyCruz, lasHeras));
        lenient().when(rutaRepository.findFirstByPreventor_IdAndFechaAndEliminadoFalse(eq("prev-1"), any()))
                .thenAnswer(inv -> Optional.ofNullable(rutaGuardada.get()));
        lenient().when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> {
            Ruta ruta = inv.getArgument(0);
            asignarId(ruta);
            rutaGuardada.set(ruta);
            return ruta;
        });
        lenient().when(paradaRepository.save(any(ParadaRuta.class))).thenAnswer(inv -> {
            ParadaRuta parada = inv.getArgument(0);
            asignarId(parada);
            return parada;
        });
    }

    @Test
    void laPrimeraConsultaDelDiaArmaLaRutaConLosPuntosDelPreventor() {
        RutaResponse ruta = service.obtenerRutaDelDia("usr-prev", null, null);

        assertThat(ruta.fecha()).isEqualTo(LocalDate.now());
        assertThat(ruta.estado()).isEqualTo(RutaEstado.PENDIENTE);
        assertThat(ruta.preventorId()).isEqualTo("prev-1");
        assertThat(ruta.paradas()).extracting(ParadaRutaResponse::nombreComercio)
                .containsExactly("Kiosco Centro", "Kiosco Godoy Cruz", "Kiosco Las Heras");
        assertThat(ruta.paradas()).extracting(ParadaRutaResponse::orden).containsExactly(1, 2, 3);
        assertThat(ruta.paradas()).noneMatch(ParadaRutaResponse::visitada);
    }

    @Test
    void desdeLaPosicionProponeElPuntoMasCercanoYElRecorridoPorVecinoMasCercano() {
        // Parado en Las Heras: lo más cerca es Las Heras, después Centro y por último Godoy Cruz.
        RutaResponse ruta = service.obtenerRutaDelDia("usr-prev", -32.8310, -68.8210);

        assertThat(ruta.siguiente().nombreComercio()).isEqualTo("Kiosco Las Heras");
        assertThat(ruta.distanciaKm()).isLessThan(0.5);
        assertThat(ruta.recorridoSugerido()).containsExactly(
                idParada(ruta, "Kiosco Las Heras"), idParada(ruta, "Kiosco Centro"), idParada(ruta, "Kiosco Godoy Cruz"));
    }

    @Test
    void alCerrarUnPedidoProponeElSiguientePuntoMasCercano() {
        service.obtenerRutaDelDia("usr-prev", null, null);

        service.registrarVisita(pedido("ped-1", centro));
        // Sin GPS se parte del último punto visitado (Centro): el más cercano es Godoy Cruz.
        RutaResponse ruta = service.obtenerRutaDelDia("usr-prev", null, null);

        assertThat(ruta.paradas()).filteredOn(ParadaRutaResponse::visitada)
                .extracting(ParadaRutaResponse::pedidoId).containsExactly("ped-1");
        assertThat(ruta.siguiente().nombreComercio()).isEqualTo("Kiosco Godoy Cruz");
        assertThat(ruta.recorridoSugerido()).hasSize(2);
        assertThat(ruta.estado()).isEqualTo(RutaEstado.PENDIENTE);
    }

    @Test
    void siguientePuntoIgnoraLasParadasVisitadas() {
        service.obtenerRutaDelDia("usr-prev", null, null);
        service.registrarVisita(pedido("ped-1", lasHeras));
        Ruta ruta = rutaGuardada.get();
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        ParadaRutaResponse siguiente = service.siguientePunto(ruta.getId(), LAS_HERAS[0], LAS_HERAS[1]);

        assertThat(siguiente.nombreComercio()).isEqualTo("Kiosco Centro");
    }

    @Test
    void conTodasLasParadasVisitadasLaRutaQuedaRealizadaYNoHaySiguiente() {
        service.obtenerRutaDelDia("usr-prev", null, null);
        service.registrarVisita(pedido("ped-1", centro));
        service.registrarVisita(pedido("ped-2", godoyCruz));
        service.registrarVisita(pedido("ped-3", lasHeras));
        Ruta ruta = rutaGuardada.get();
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        assertThat(ruta.getEstado()).isEqualTo(RutaEstado.REALIZADA);
        assertThat(service.siguientePunto(ruta.getId(), CENTRO[0], CENTRO[1])).isNull();
        assertThat(service.obtenerRutaDelDia("usr-prev", CENTRO[0], CENTRO[1]).siguiente()).isNull();
    }

    @Test
    void unSegundoPedidoDelMismoComercioNoSePierde() {
        service.obtenerRutaDelDia("usr-prev", null, null);
        service.registrarVisita(pedido("ped-1", centro));
        service.registrarVisita(pedido("ped-2", centro));

        RutaResponse ruta = service.obtenerRutaDelDia("usr-prev", null, null);

        assertThat(ruta.paradas()).filteredOn(ParadaRutaResponse::visitada)
                .extracting(ParadaRutaResponse::pedidoId).containsExactly("ped-1", "ped-2");
        assertThat(ruta.paradas()).hasSize(4);
    }

    @Test
    void losPuntosDeMinoristasDadosDeBajaNoEntranEnLaRuta() {
        lasHeras.getMinorista().setEliminado(true);

        RutaResponse ruta = service.obtenerRutaDelDia("usr-prev", null, null);

        assertThat(ruta.paradas()).extracting(ParadaRutaResponse::nombreComercio)
                .containsExactly("Kiosco Centro", "Kiosco Godoy Cruz");
    }

    @Test
    void unaPosicionInvalidaSeRechaza() {
        assertThatThrownBy(() -> service.obtenerRutaDelDia("usr-prev", 120.0, -68.84))
                .isInstanceOf(ErrorServiceException.class).hasMessage("La posición actual no es válida.");
        assertThatThrownBy(() -> service.obtenerRutaDelDia("usr-prev", -32.0, null))
                .isInstanceOf(ErrorServiceException.class);
    }

    @Test
    void siguientePuntoDeUnaRutaInexistenteDevuelve404() {
        when(rutaRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.siguientePunto("no-existe", CENTRO[0], CENTRO[1]))
                .isInstanceOf(RecursoNoEncontradoException.class);
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

    private Pedido pedido(String id, PuntoDeVenta punto) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        ReflectionTestUtils.setField(pedido, "fechaHoraCarga", LocalDateTime.now().plusSeconds(pedidosCargados.size()));
        pedidosCargados.add(pedido);
        return pedido;
    }

    private final List<Pedido> pedidosCargados = new ArrayList<>();

    private static String idParada(RutaResponse ruta, String comercio) {
        return ruta.paradas().stream().filter(p -> p.nombreComercio().equals(comercio)).findFirst()
                .orElseThrow().id();
    }

    private static void asignarId(com.dulcemayor.entity.BaseEntity entidad) {
        if (entidad.getId() == null) {
            entidad.setId(UUID.randomUUID().toString());
        }
    }
}
