package com.dulcemayor.service;

import com.dulcemayor.dto.ParadaRutaResponse;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.dto.mapper.ParadaRutaMapper;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Distribuidor;
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
import com.dulcemayor.utils.DistanciaGeografica;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E5-01: rutas del día y cálculo del próximo punto más cercano.
 *
 * <p>Implementa los métodos del UML de {@code Ruta}/{@code ParadaRuta} y el contrato del kickoff
 * {@link #siguientePunto(String, double, double)} que consume E5-02.</p>
 *
 * <p>NOTA (E5-01): ningún issue del plan da de alta rutas, así que la ruta del preventor se arma sola
 * ({@link #obtenerRutaDelDia}): una por preventor y por día, con una parada por cada punto de venta
 * activo que tiene asignado. Los puntos que se dan de alta durante el día se suman como paradas nuevas
 * en la próxima consulta. Una parada queda "visitada" cuando tiene pedido ({@link #registrarVisita}).
 * La ruta de reparto del distribuidor la arma E5-02 ({@link #rutaDeRepartoDelDia}).</p>
 *
 * <p>NOTA (E5-02): el criterio de "parada pendiente" quedó en {@link ParadaRuta#estaPendiente()} y depende
 * del tipo de ruta: sin pedido en la del preventor, pedido no entregado en la de reparto. Así
 * {@link #siguientePunto}, el recorrido sugerido y {@code visitada} sirven para las dos.</p>
 *
 * <p>NOTA (E5-01): {@code ParadaRuta.orden} es el orden de alta de la parada. El orden de visita
 * sugerido se calcula en cada consulta (vecino más cercano desde la posición) y no se persiste.</p>
 */
@Service
@Transactional(readOnly = true)
public class RutaService {

    private static final String RUTA_NO_ENCONTRADA = "No se encontró la ruta.";

    private final RutaRepository rutaRepository;
    private final ParadaRutaRepository paradaRepository;
    private final PuntoDeVentaRepository puntoRepository;
    private final PedidoRepository pedidoRepository;
    private final PreventorRepository preventorRepository;
    private final DistribuidorRepository distribuidorRepository;
    private final PreventorService preventorService;
    private final ParadaRutaMapper mapper;

    public RutaService(RutaRepository rutaRepository, ParadaRutaRepository paradaRepository,
            PuntoDeVentaRepository puntoRepository, PedidoRepository pedidoRepository,
            PreventorRepository preventorRepository, DistribuidorRepository distribuidorRepository,
            PreventorService preventorService, ParadaRutaMapper mapper) {
        this.rutaRepository = rutaRepository;
        this.paradaRepository = paradaRepository;
        this.puntoRepository = puntoRepository;
        this.pedidoRepository = pedidoRepository;
        this.preventorRepository = preventorRepository;
        this.distribuidorRepository = distribuidorRepository;
        this.preventorService = preventorService;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- métodos del UML

    @Transactional
    public RutaResponse crearRuta(LocalDate fecha, String idPreventor, String idDistribuidor) {
        if (fecha == null) {
            throw new ErrorServiceException("Indicá la fecha de la ruta.", "fecha");
        }
        Ruta ruta = new Ruta();
        ruta.setFecha(fecha);
        ruta.setEstado(RutaEstado.PENDIENTE);
        if (idPreventor != null && !idPreventor.isBlank()) {
            ruta.setPreventor(preventorRepository.findById(idPreventor).filter(p -> !p.isEliminado())
                    .orElseThrow(() -> new ErrorServiceException("El preventor no existe.", "idPreventor")));
        }
        if (idDistribuidor != null && !idDistribuidor.isBlank()) {
            ruta.setDistribuidor(distribuidorRepository.findById(idDistribuidor).filter(d -> !d.isEliminado())
                    .orElseThrow(() -> new ErrorServiceException("El distribuidor no existe.", "idDistribuidor")));
        }
        return aRespuesta(rutaRepository.save(ruta), null, null);
    }

    public RutaResponse buscarRuta(String id) {
        return aRespuesta(buscarEntidad(id), null, null);
    }

    @Transactional
    public RutaResponse cambiarEstado(String id, RutaEstado estado) {
        if (estado == null) {
            throw new ErrorServiceException("Indicá el estado de la ruta.", "estado");
        }
        Ruta ruta = buscarEntidad(id);
        ruta.setEstado(estado);
        return aRespuesta(ruta, null, null);
    }

    @Transactional
    public ParadaRutaResponse agregarParada(String idRuta, String idPuntoDeVenta, String idPedido, int orden) {
        Ruta ruta = buscarEntidad(idRuta);
        PuntoDeVenta punto = puntoRepository.findByIdAndEliminadoFalse(idPuntoDeVenta)
                .orElseThrow(() -> new ErrorServiceException("El punto de venta no existe.", "idPuntoDeVenta"));
        ParadaRuta parada = new ParadaRuta();
        parada.setPuntoDeVenta(punto);
        parada.setOrden(orden);
        if (idPedido != null && !idPedido.isBlank()) {
            parada.setPedido(pedidoRepository.findById(idPedido).filter(p -> !p.isEliminado())
                    .orElseThrow(() -> new ErrorServiceException("El pedido no existe.", "idPedido")));
        }
        ruta.agregarParada(parada);
        return mapper.aRespuesta(paradaRepository.save(parada));
    }

    public List<RutaResponse> listarRutaPorFecha(LocalDate fecha) {
        return rutaRepository.findByFechaAndEliminadoFalse(fecha).stream().map(r -> aRespuesta(r, null, null)).toList();
    }

    public List<RutaResponse> listarRutaPorPreventor(String idPreventor) {
        return rutaRepository.findByPreventor_IdAndEliminadoFalse(idPreventor).stream()
                .map(r -> aRespuesta(r, null, null)).toList();
    }

    public List<RutaResponse> listarRutaPorDistribuidor(String idDistribuidor) {
        return rutaRepository.findByDistribuidor_IdAndEliminadoFalse(idDistribuidor).stream()
                .map(r -> aRespuesta(r, null, null)).toList();
    }

    public List<ParadaRutaResponse> listarParadaPorRuta(String idRuta) {
        return mapper.aRespuestas(paradaRepository.findByRuta_IdAndEliminadoFalseOrderByOrdenAsc(idRuta));
    }

    // ---------------------------------------------------------------- E5-01

    /**
     * Contrato del kickoff (E5-01 → E5-02): la parada pendiente más cercana a la posición.
     *
     * @return la parada pendiente más cercana, o {@code null} si la ruta no tiene paradas pendientes
     */
    public ParadaRutaResponse siguientePunto(String idRuta, double latitud, double longitud) {
        validarPosicion(latitud, longitud);
        ParadaRuta siguiente = masCercana(pendientes(buscarEntidad(idRuta)), latitud, longitud);
        return siguiente == null ? null : mapper.aRespuesta(siguiente);
    }

    /**
     * La ruta de hoy del preventor autenticado; si no existe la crea con sus puntos de venta.
     * Con posición calcula el siguiente punto y el recorrido sugerido desde ahí; sin posición, desde
     * el último punto visitado (o desde la primera parada pendiente si todavía no visitó ninguno).
     */
    @Transactional
    public RutaResponse obtenerRutaDelDia(String idUsuarioPreventor, Double latitud, Double longitud) {
        boolean conPosicion = latitud != null || longitud != null;
        if (conPosicion) {
            validarPosicion(latitud, longitud);
        }
        // El bloqueo del perfil serializa la creación de la ruta con la carga de pedidos del mismo preventor.
        Preventor preventor = preventorService.bloquearPerfilParaCarga(idUsuarioPreventor);
        Ruta ruta = rutaDelDia(preventor);
        if (conPosicion) {
            return aRespuesta(ruta, latitud, longitud);
        }
        return respuestaSinPosicion(ruta, ultimoVisitado(ruta));
    }

    /**
     * Marca como visitada la parada del punto del pedido en la ruta de hoy de su preventor.
     * Lo llama {@link PreventaService#cargarPedido} dentro de su transacción (con el preventor ya
     * bloqueado). Si el punto no estaba en la ruta se agrega; si ya estaba visitado (segundo pedido
     * del mismo comercio en el día) se agrega otra parada visitada para no perder el pedido.
     */
    @Transactional
    public void registrarVisita(Pedido pedido) {
        Ruta ruta = rutaDelDia(pedido.getPreventor());
        String idPunto = pedido.getPuntoDeVenta().getId();
        ParadaRuta parada = paradasActivas(ruta).stream()
                .filter(p -> p.getPedido() == null && idPunto.equals(p.getPuntoDeVenta().getId()))
                .findFirst()
                .orElseGet(() -> nuevaParada(ruta, pedido.getPuntoDeVenta()));
        parada.setPedido(pedido);
        if (pendientes(ruta).isEmpty()) {
            ruta.setEstado(RutaEstado.REALIZADA);
        }
    }

    // ---------------------------------------------------------------- E5-02 (reparto)

    /**
     * E5-02: la ruta de reparto de hoy del distribuidor; si no existe la crea. Los pedidos que quedaron
     * sin entregar en rutas de reparto de días anteriores pasan a la de hoy y esas rutas quedan INCOMPLETA.
     * El llamador tiene que tener bloqueado el perfil del distribuidor para no crear dos rutas del mismo día.
     */
    @Transactional
    public Ruta rutaDeRepartoDelDia(Distribuidor distribuidor) {
        LocalDate hoy = LocalDate.now();
        Ruta ruta = rutaRepository.findFirstByDistribuidor_IdAndFechaAndEliminadoFalse(distribuidor.getId(), hoy)
                .orElseGet(() -> {
                    Ruta nueva = new Ruta();
                    nueva.setFecha(hoy);
                    nueva.setEstado(RutaEstado.PENDIENTE);
                    nueva.setDistribuidor(distribuidor);
                    return rutaRepository.save(nueva);
                });
        for (Ruta anterior : rutaRepository.findByDistribuidor_IdAndFechaBeforeAndEstadoAndEliminadoFalse(
                distribuidor.getId(), hoy, RutaEstado.PENDIENTE)) {
            List<ParadaRuta> sinEntregar = pendientes(anterior);
            if (!sinEntregar.isEmpty()) {
                sinEntregar.forEach(parada -> agregarAReparto(ruta, parada.getPedido()));
                anterior.setEstado(RutaEstado.INCOMPLETA);
            }
        }
        return ruta;
    }

    /** E5-02: suma el pedido como parada de la ruta de reparto (si ya estaba, devuelve esa parada). */
    @Transactional
    public ParadaRuta agregarAReparto(Ruta ruta, Pedido pedido) {
        ParadaRuta existente = paradaDelPedido(ruta, pedido.getId()).orElse(null);
        if (existente != null) {
            return existente;
        }
        ParadaRuta parada = nuevaParada(ruta, pedido.getPuntoDeVenta());
        parada.setPedido(pedido);
        if (ruta.getEstado() == RutaEstado.REALIZADA) {
            ruta.setEstado(RutaEstado.PENDIENTE);
        }
        return parada;
    }

    /** E5-02: la parada activa de la ruta que lleva ese pedido. */
    public Optional<ParadaRuta> paradaDelPedido(Ruta ruta, String idPedido) {
        return paradasActivas(ruta).stream()
                .filter(p -> p.getPedido() != null && p.getPedido().getId().equals(idPedido))
                .findFirst();
    }

    /**
     * E5-02: se llama después de pasar el pedido de la parada a ENTREGADO. Propone la parada pendiente más
     * cercana a la posición del distribuidor (o, sin GPS, al comercio recién entregado). Si no quedan
     * pendientes, la ruta queda REALIZADA y devuelve null.
     */
    @Transactional
    public ParadaRuta registrarEntrega(Ruta ruta, ParadaRuta entregada, Double latitud, Double longitud) {
        double[] desde;
        if (latitud != null || longitud != null) {
            validarPosicion(latitud, longitud);
            desde = new double[] {latitud, longitud};
        } else {
            desde = coordenadas(entregada);
        }
        List<ParadaRuta> pendientes = pendientes(ruta);
        if (pendientes.isEmpty()) {
            ruta.setEstado(RutaEstado.REALIZADA);
            return null;
        }
        return masCercana(pendientes, desde[0], desde[1]);
    }

    /**
     * E5-02: la ruta de reparto como respuesta. Con posición calcula desde ahí; sin posición, desde
     * {@code origen} (la última parada entregada) o, si no hay, desde la primera parada pendiente.
     */
    public RutaResponse respuestaDeReparto(Ruta ruta, Double latitud, Double longitud, ParadaRuta origen) {
        if (latitud != null || longitud != null) {
            validarPosicion(latitud, longitud);
            return aRespuesta(ruta, latitud, longitud);
        }
        return respuestaSinPosicion(ruta, origen == null ? null : coordenadas(origen));
    }

    // ---------------------------------------------------------------- auxiliares

    /**
     * Sin GPS: desde el último punto hecho; si no hay, el recorrido empieza en la primera parada pendiente
     * y no hay distancia que informar.
     */
    private RutaResponse respuestaSinPosicion(Ruta ruta, double[] ultimo) {
        if (ultimo != null) {
            return aRespuesta(ruta, ultimo[0], ultimo[1]);
        }
        double[] primera = pendientes(ruta).stream().findFirst().map(RutaService::coordenadas).orElse(null);
        RutaResponse respuesta = aRespuesta(ruta, primera == null ? null : primera[0], primera == null ? null : primera[1]);
        return new RutaResponse(respuesta.id(), respuesta.fecha(), respuesta.estado(), respuesta.preventorId(),
                respuesta.distribuidorId(), respuesta.paradas(), respuesta.siguiente(), null,
                respuesta.recorridoSugerido());
    }

    private Ruta buscarEntidad(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException(RUTA_NO_ENCONTRADA);
        }
        return rutaRepository.findById(id).filter(r -> !r.isEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoException(RUTA_NO_ENCONTRADA));
    }

    /** Busca o crea la ruta de hoy y le suma como paradas los puntos activos que todavía no tiene. */
    private Ruta rutaDelDia(Preventor preventor) {
        Ruta ruta = rutaRepository.findFirstByPreventor_IdAndFechaAndEliminadoFalse(preventor.getId(), LocalDate.now())
                .orElseGet(() -> {
                    Ruta nueva = new Ruta();
                    nueva.setFecha(LocalDate.now());
                    nueva.setEstado(RutaEstado.PENDIENTE);
                    nueva.setPreventor(preventor);
                    return rutaRepository.save(nueva);
                });
        Set<String> enRuta = new HashSet<>();
        paradasActivas(ruta).forEach(p -> enRuta.add(p.getPuntoDeVenta().getId()));
        boolean agregoPendientes = false;
        for (PuntoDeVenta punto : puntoRepository.findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(
                preventor.getId())) {
            if (!punto.getMinorista().isEliminado() && enRuta.add(punto.getId())) {
                nuevaParada(ruta, punto);
                agregoPendientes = true;
            }
        }
        if (agregoPendientes && ruta.getEstado() == RutaEstado.REALIZADA) {
            ruta.setEstado(RutaEstado.PENDIENTE);
        }
        return ruta;
    }

    private ParadaRuta nuevaParada(Ruta ruta, PuntoDeVenta punto) {
        ParadaRuta parada = new ParadaRuta();
        parada.setPuntoDeVenta(punto);
        parada.setOrden(ruta.getParadas().stream().mapToInt(ParadaRuta::getOrden).max().orElse(0) + 1);
        ruta.agregarParada(parada);
        return paradaRepository.save(parada);
    }

    private static List<ParadaRuta> paradasActivas(Ruta ruta) {
        return ruta.getParadas().stream().filter(p -> !p.isEliminado()).toList();
    }

    private static List<ParadaRuta> pendientes(Ruta ruta) {
        return paradasActivas(ruta).stream().filter(ParadaRuta::estaPendiente).toList();
    }

    /** Sin GPS se parte del último punto visitado (el del último pedido cargado); null si no visitó ninguno. */
    private static double[] ultimoVisitado(Ruta ruta) {
        return paradasActivas(ruta).stream()
                .filter(p -> p.getPedido() != null && p.getPedido().getFechaHoraCarga() != null)
                .max(Comparator.comparing((ParadaRuta p) -> p.getPedido().getFechaHoraCarga()))
                .map(RutaService::coordenadas)
                .orElse(null);
    }

    private static double[] coordenadas(ParadaRuta parada) {
        Direccion direccion = parada.getPuntoDeVenta().getDireccion();
        return new double[] {direccion.getLatitud(), direccion.getLongitud()};
    }

    private static double distancia(ParadaRuta parada, double latitud, double longitud) {
        double[] destino = coordenadas(parada);
        return DistanciaGeografica.kilometros(latitud, longitud, destino[0], destino[1]);
    }

    /** La más cercana; ante empate gana la de menor orden, para que el resultado sea estable. */
    private static ParadaRuta masCercana(List<ParadaRuta> paradas, double latitud, double longitud) {
        return paradas.stream()
                .min(Comparator.comparingDouble((ParadaRuta p) -> distancia(p, latitud, longitud))
                        .thenComparingInt(ParadaRuta::getOrden))
                .orElse(null);
    }

    /** Heurística del vecino más cercano: desde el origen, siempre a la parada pendiente más próxima. */
    static List<ParadaRuta> recorridoSugerido(List<ParadaRuta> pendientes, double latitud, double longitud) {
        List<ParadaRuta> restantes = new ArrayList<>(pendientes);
        List<ParadaRuta> recorrido = new ArrayList<>();
        double lat = latitud;
        double lon = longitud;
        while (!restantes.isEmpty()) {
            ParadaRuta proxima = masCercana(restantes, lat, lon);
            recorrido.add(proxima);
            restantes.remove(proxima);
            double[] punto = coordenadas(proxima);
            lat = punto[0];
            lon = punto[1];
        }
        return recorrido;
    }

    private static void validarPosicion(Double latitud, Double longitud) {
        if (!DistanciaGeografica.coordenadasValidas(latitud, longitud)) {
            throw new ErrorServiceException("La posición actual no es válida.", "latitud");
        }
    }

    private RutaResponse aRespuesta(Ruta ruta, Double latitud, Double longitud) {
        List<ParadaRuta> paradas = paradasActivas(ruta).stream()
                .sorted(Comparator.comparingInt(ParadaRuta::getOrden)).toList();
        ParadaRutaResponse siguiente = null;
        Double distanciaKm = null;
        List<String> recorrido = List.of();
        if (latitud != null && longitud != null) {
            List<ParadaRuta> sugerido = recorridoSugerido(pendientes(ruta), latitud, longitud);
            recorrido = sugerido.stream().map(ParadaRuta::getId).filter(Objects::nonNull).toList();
            if (!sugerido.isEmpty()) {
                siguiente = mapper.aRespuesta(sugerido.get(0));
                distanciaKm = Math.round(distancia(sugerido.get(0), latitud, longitud) * 100) / 100.0;
            }
        }
        Preventor preventor = ruta.getPreventor();
        Distribuidor distribuidor = ruta.getDistribuidor();
        return new RutaResponse(ruta.getId(), ruta.getFecha(), ruta.getEstado(),
                preventor == null ? null : preventor.getId(), distribuidor == null ? null : distribuidor.getId(),
                mapper.aRespuestas(paradas), siguiente, distanciaKm, recorrido);
    }
}
