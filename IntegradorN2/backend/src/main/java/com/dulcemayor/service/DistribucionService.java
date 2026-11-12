package com.dulcemayor.service;

import com.dulcemayor.dto.EntregaRequest;
import com.dulcemayor.dto.EntregaResponse;
import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.dto.PedidoAlmacenResponse;
import com.dulcemayor.dto.PedidoListoRepartoResponse;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.dto.TomarPedidosRequest;
import com.dulcemayor.dto.mapper.ParadaRutaMapper;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.ParadaRuta;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Ruta;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E5-02: portal del distribuidor. Toma los pedidos listos para reparto, arma su ruta de reparto del día
 * y registra las entregas (State de E4-01), proponiendo el siguiente punto (E5-01) y avisándole por
 * correo al próximo comercio.
 *
 * <p>NOTA (E5-02): ningún issue anterior pasaba los pedidos de FACTURADO a EN_REPARTO. Lo hace
 * {@link #tomarPedidos}: el distribuidor elige del listado de pedidos listos (FACTURADO con la factura
 * PAGADA, porque E4-05 deja el pedido "pendiente de envío" recién al pagarse) y queda como transportista.
 * Si en el futuro el almacén asigna los repartos, tiene que llamar a {@code PedidoService.avanzar(...,
 * INICIAR_REPARTO, ...)} y a {@link RutaService#agregarAReparto} de la misma forma.</p>
 */
@Service
@Transactional(readOnly = true)
public class DistribucionService {

    private static final String PEDIDO_NO_ENCONTRADO = "No se encontró el pedido.";
    private static final String FUERA_DE_RUTA = "El pedido no está en tu ruta de reparto de hoy.";

    private final DistribuidorService distribuidores;
    private final RutaService rutas;
    private final PedidoService pedidoService;
    private final PedidoAlmacenService pedidoAlmacen;
    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final HistorialEstadoPedidoRepository historial;
    private final ParadaRutaMapper paradaMapper;
    private final ApplicationEventPublisher eventos;

    public DistribucionService(DistribuidorService distribuidores, RutaService rutas, PedidoService pedidoService,
            PedidoAlmacenService pedidoAlmacen, PedidoRepository pedidos, FacturaRepository facturas,
            HistorialEstadoPedidoRepository historial, ParadaRutaMapper paradaMapper,
            ApplicationEventPublisher eventos) {
        this.distribuidores = distribuidores;
        this.rutas = rutas;
        this.pedidoService = pedidoService;
        this.pedidoAlmacen = pedidoAlmacen;
        this.pedidos = pedidos;
        this.facturas = facturas;
        this.historial = historial;
        this.paradaMapper = paradaMapper;
        this.eventos = eventos;
    }

    /** Pedidos FACTURADO con la factura PAGADA que todavía nadie tomó para repartir. */
    public List<PedidoListoRepartoResponse> listarPedidosListos() {
        return facturas.findByEstadoAndEliminadoFalseOrderByNumeroFacturaDesc(EstadoFactura.PAGADA).stream()
                .filter(factura -> !factura.getPedido().isEliminado()
                        && factura.getPedido().getEstado() == EstadoPedido.FACTURADO)
                .map(DistribucionService::listo)
                .toList();
    }

    /**
     * Pasa los pedidos a EN_REPARTO con el distribuidor autenticado como transportista y los suma a su ruta
     * del día. Es todo o nada: si un pedido no está listo, no se toma ninguno.
     */
    @Transactional
    public RutaResponse tomarPedidos(String idUsuario, TomarPedidosRequest request) {
        if (request == null || request.pedidoIds() == null || request.pedidoIds().isEmpty()) {
            throw new ErrorServiceException("Elegí al menos un pedido para repartir.", "pedidoIds");
        }
        Distribuidor distribuidor = distribuidores.bloquearPerfilParaReparto(idUsuario);
        Ruta ruta = rutas.rutaDeRepartoDelDia(distribuidor);
        for (String idPedido : new LinkedHashSet<>(request.pedidoIds())) {
            // El bloqueo va antes de validar: si otro distribuidor lo tomó recién, se ve el estado nuevo.
            Pedido pedido = pedidos.buscarActivoParaActualizar(idPedido)
                    .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
            validarListoParaReparto(pedido);
            pedidoService.avanzar(idPedido, EventoPedido.INICIAR_REPARTO, idUsuario, null, distribuidor.getId());
            rutas.agregarAReparto(ruta, pedido);
        }
        return rutas.respuestaDeReparto(ruta, null, null, ultimaEntregada(distribuidor, ruta));
    }

    /** Ruta de reparto de hoy con el siguiente punto desde la posición (o desde la última entrega). */
    @Transactional
    public RutaResponse obtenerRutaDelDia(String idUsuario, Double latitud, Double longitud) {
        Distribuidor distribuidor = distribuidores.bloquearPerfilParaReparto(idUsuario);
        Ruta ruta = rutas.rutaDeRepartoDelDia(distribuidor);
        boolean conPosicion = latitud != null || longitud != null;
        return rutas.respuestaDeReparto(ruta, latitud, longitud,
                conPosicion ? null : ultimaEntregada(distribuidor, ruta));
    }

    /** Detalle de un pedido de su ruta de hoy (productos a entregar). 404 si no está en su ruta. */
    @Transactional
    public PedidoAlmacenResponse buscarPedido(String idUsuario, String idPedido) {
        Distribuidor distribuidor = distribuidores.bloquearPerfilParaReparto(idUsuario);
        Ruta ruta = rutas.rutaDeRepartoDelDia(distribuidor);
        rutas.paradaDelPedido(ruta, idPedido).orElseThrow(() -> new RecursoNoEncontradoException(FUERA_DE_RUTA));
        return pedidoAlmacen.buscar(idPedido);
    }

    /**
     * Marca el pedido como ENTREGADO (registra fecha, usuario y transportista en el historial), propone el
     * siguiente punto más cercano y, si ese comercio no fue avisado todavía, publica el aviso por correo.
     */
    @Transactional
    public EntregaResponse entregar(String idUsuario, String idPedido, EntregaRequest request) {
        Double latitud = request == null ? null : request.latitud();
        Double longitud = request == null ? null : request.longitud();
        String observacion = request == null ? null : request.observacion();

        Distribuidor distribuidor = distribuidores.bloquearPerfilParaReparto(idUsuario);
        Ruta ruta = rutas.rutaDeRepartoDelDia(distribuidor);
        ParadaRuta entregada = rutas.paradaDelPedido(ruta, idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException(FUERA_DE_RUTA));

        HistorialEstadoPedidoResponse registro = pedidoService.avanzar(
                idPedido, EventoPedido.ENTREGAR, idUsuario, observacion, distribuidor.getId());
        ParadaRuta siguiente = rutas.registrarEntrega(ruta, entregada, latitud, longitud);

        String avisado = null;
        if (siguiente != null && siguiente.getFechaAvisoEnCamino() == null) {
            siguiente.setFechaAvisoEnCamino(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
            eventos.publishEvent(new PedidoEnCaminoEvent(siguiente.getPedido().getId(),
                    (distribuidor.getNombre() + " " + distribuidor.getApellido()).strip()));
            avisado = siguiente.getPuntoDeVenta().getNombreComercio();
        }
        boolean conPosicion = latitud != null || longitud != null;
        RutaResponse ruteo = rutas.respuestaDeReparto(ruta, latitud, longitud, conPosicion ? null : entregada);
        return new EntregaResponse(registro, ruteo, siguiente == null ? null : paradaMapper.aRespuesta(siguiente),
                avisado);
    }

    // ---------------------------------------------------------------- auxiliares

    private void validarListoParaReparto(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.FACTURADO) {
            throw new ErrorServiceException("El pedido N° " + pedido.getNumeroPedido()
                    + " no está listo para reparto: está en " + pedido.getEstado() + ".", "pedidoIds");
        }
        boolean pagada = facturas.findByPedido_IdAndEliminadoFalse(pedido.getId())
                .map(Factura::getEstado).filter(estado -> estado == EstadoFactura.PAGADA).isPresent();
        if (!pagada) {
            throw new ErrorServiceException("El pedido N° " + pedido.getNumeroPedido()
                    + " todavía no está pagado.", "pedidoIds");
        }
    }

    /** La parada de la última entrega del distribuidor, si es de la ruta de hoy (origen del recorrido sin GPS). */
    private ParadaRuta ultimaEntregada(Distribuidor distribuidor, Ruta ruta) {
        return historial.findFirstByTransportista_IdAndEstadoAndEliminadoFalseOrderByFechaHoraDescIdDesc(
                        distribuidor.getId(), EstadoPedido.ENTREGADO)
                .flatMap(registro -> rutas.paradaDelPedido(ruta, registro.getPedido().getId()))
                .orElse(null);
    }

    private static PedidoListoRepartoResponse listo(Factura factura) {
        Pedido pedido = factura.getPedido();
        PuntoDeVenta punto = pedido.getPuntoDeVenta();
        Direccion direccion = punto.getDireccion();
        return new PedidoListoRepartoResponse(pedido.getId(), pedido.getNumeroPedido(), factura.getNumeroFactura(),
                punto.getNombreComercio(), direccion == null ? null : direccion.getCalle(),
                direccion == null ? null : direccion.getNumeracion(),
                direccion == null || direccion.getLocalidad() == null ? null : direccion.getLocalidad().getNombre(),
                pedido.getTotal());
    }
}
