package com.dulcemayor.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.dulcemayor.dto.PagoRequest;
import com.dulcemayor.dto.PedidoPreventaRequest;
import com.dulcemayor.dto.PedidoPreventaResponse;
import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.ResultadoFacturacionResponse;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.dto.TomarPedidosRequest;
import com.dulcemayor.dto.VigenciaPrecioRequest;
import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pago;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.VigenciaPrecio;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.entity.enums.MonedaPago;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.entity.enums.TipoEmpresa;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.FormaDePagoRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MayoristaRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PagoRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;
import com.dulcemayor.service.DistribucionService;
import com.dulcemayor.service.FacturacionService;
import com.dulcemayor.service.ImagenService;
import com.dulcemayor.service.PagoService;
import com.dulcemayor.service.PreventaService;
import com.dulcemayor.service.PuntoDeVentaService;
import com.dulcemayor.service.StockService;
import com.dulcemayor.service.SucursalService;
import com.dulcemayor.service.UsuarioService;
import com.dulcemayor.service.VigenciaPrecioService;

/**
 * E6-04: datos de demostración. Los llama {@link DataSeeder} después de los datos base, solo con la base vacía
 * y si {@code app.seeder.demo=true} (por defecto). Carga ~20 productos con imagen e historial de precios,
 * 4 sucursales con foto, 3 preventores, minoristas con comercios, seis meses de ventas para los gráficos y
 * pedidos de hoy en todos los estados.
 *
 * <p>NOTA (E6-04): el historial (pedidos, facturas, pagos y vigencias con fechas pasadas) se guarda por
 * repositorio porque los services siempre fechan "ahora" y no aceptan vigencias anteriores a hoy. Las
 * transiciones de estado igual pasan por {@code Pedido.registrarCarga} y {@code Pedido.avanzar} (patrón State),
 * así el historial queda consistente. Los pedidos de hoy sí se cargan con los services reales (preventa,
 * facturación, pago y reparto), igual que desde las pantallas. Las ventas históricas no generan movimientos
 * de stock: el stock que se ve es el saldo actual después de esas ventas.
 *
 * <p>Los tests corren con {@code app.seeder.demo=false} (application-test.properties) porque varios
 * dependen de los cinco productos base de {@link DataSeeder}; {@code DatosDemostracionIntegrationTest}
 * prueba esta carga completa.
 */
@Component
public class DatosDemostracion {

    private static final Logger log = LoggerFactory.getLogger(DatosDemostracion.class);
    private static final int DIAS_DE_HISTORIA = 180;
    /** Desde este día hacia atrás rige el primer precio (80 %); después, el segundo (90 %); hoy, el actual. */
    private static final int DIAS_PRIMER_PRECIO = 120;

    private final UsuarioService usuarios;
    private final ImagenService imagenes;
    private final SucursalService sucursales;
    private final PuntoDeVentaService puntosService;
    private final VigenciaPrecioService vigenciaService;
    private final StockService stock;
    private final PreventaService preventa;
    private final FacturacionService facturacion;
    private final PagoService pagos;
    private final DistribucionService distribucion;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final VigenciaPrecioRepository vigenciaRepository;
    private final LocalidadRepository localidadRepository;
    private final MayoristaRepository mayoristaRepository;
    private final PreventorRepository preventorRepository;
    private final MinoristaRepository minoristaRepository;
    private final DistribuidorRepository distribuidorRepository;
    private final PuntoDeVentaRepository puntoRepository;
    private final FormaDePagoRepository formaDePagoRepository;
    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoPedidoRepository historialRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;

    private final Random azar = new Random(2026);
    private final Set<Long> numerosPedido = new HashSet<>();

    public DatosDemostracion(UsuarioService usuarios, ImagenService imagenes, SucursalService sucursales,
            PuntoDeVentaService puntosService, VigenciaPrecioService vigenciaService, StockService stock,
            PreventaService preventa, FacturacionService facturacion, PagoService pagos,
            DistribucionService distribucion, CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository, VigenciaPrecioRepository vigenciaRepository,
            LocalidadRepository localidadRepository, MayoristaRepository mayoristaRepository,
            PreventorRepository preventorRepository, MinoristaRepository minoristaRepository,
            DistribuidorRepository distribuidorRepository, PuntoDeVentaRepository puntoRepository,
            FormaDePagoRepository formaDePagoRepository, PedidoRepository pedidoRepository,
            HistorialEstadoPedidoRepository historialRepository, FacturaRepository facturaRepository,
            PagoRepository pagoRepository) {
        this.usuarios = usuarios;
        this.imagenes = imagenes;
        this.sucursales = sucursales;
        this.puntosService = puntosService;
        this.vigenciaService = vigenciaService;
        this.stock = stock;
        this.preventa = preventa;
        this.facturacion = facturacion;
        this.pagos = pagos;
        this.distribucion = distribucion;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.vigenciaRepository = vigenciaRepository;
        this.localidadRepository = localidadRepository;
        this.mayoristaRepository = mayoristaRepository;
        this.preventorRepository = preventorRepository;
        this.minoristaRepository = minoristaRepository;
        this.distribuidorRepository = distribuidorRepository;
        this.puntoRepository = puntoRepository;
        this.formaDePagoRepository = formaDePagoRepository;
        this.pedidoRepository = pedidoRepository;
        this.historialRepository = historialRepository;
        this.facturaRepository = facturaRepository;
        this.pagoRepository = pagoRepository;
    }

    /** Se llama dentro de la transacción del seeder, después de los datos base. */
    public void cargar() {
        log.info("Cargando datos de demostración (E6-04).");
        cargarSucursales();
        cargarProductos();
        cargarHistorialDePrecios();
        cargarPreventoresYComercios();
        cargarVentasHistoricas();
        cargarPedidosDeHoy();
    }

    // ---------------------------------------------------------------- empresa

    /** Tres sucursales más (la Casa Central la crea DataSeeder), cada una con su foto. */
    private void cargarSucursales() {
        String mayoristaId = mayoristaRepository.findByEliminadoFalse().get(0).getId();
        crearSucursal(mayoristaId, "Sucursal Godoy Cruz", "5501", "San Martín Sur", "450", -32.9271, -68.8453,
                "sucursal-2");
        crearSucursal(mayoristaId, "Sucursal Guaymallén", "5521", "Bandera de los Andes", "1900", -32.8936,
                -68.8064, "sucursal-3");
        crearSucursal(mayoristaId, "Sucursal Maipú", "5515", "Padre Vázquez", "600", -32.9831, -68.7871,
                "sucursal-4");
    }

    private void crearSucursal(String mayoristaId, String nombre, String codigoPostal, String calle, String numero,
            double latitud, double longitud, String foto) {
        var localidad = localidadRepository.findByCodigoPostalAndEliminadoFalse(codigoPostal).orElseThrow();
        sucursales.crearSucursal(new SucursalRequest(nombre, TipoEmpresa.SUCURSAL, mayoristaId, localidad.getId(),
                calle, numero, null, null, null, "Atención a comercios de 8 a 17 h", latitud, longitud,
                imagen("imagenes/demo/" + foto + ".png", TipoImagen.SUCURSAL)));
    }

    // ---------------------------------------------------------------- catálogo

    /** 15 productos más, para llegar a 20 entre los cinco base y estos. */
    private void cargarProductos() {
        producto("ALF-002", "Alfajor de maicena", "Relleno de dulce de leche y coco rallado.", "Caja x 12", 12,
                "Alfajores", 5200, 120, false);
        producto("ALF-003", "Alfajor triple blanco", "Tres tapas con baño de chocolate blanco.", "Caja x 6", 6,
                "Alfajores", 6800, 80, true);
        producto("ALF-004", "Alfajor de fruta", "Relleno de membrillo, bañado en glasé.", "Caja x 12", 12,
                "Alfajores", 4500, 95, false);
        producto("CAR-002", "Caramelos de miel", "Caramelos duros de miel, envueltos de a uno.", "Bolsa x 1 kg", 1,
                "Caramelos", 3900, 140, false);
        producto("CAR-003", "Masticables surtidos", "Frutilla, naranja, limón y ananá.", "Bolsa x 800 g", 1,
                "Caramelos", 3600, 160, true);
        producto("CAR-004", "Pastillas de menta", "Pastillas refrescantes en caja individual.", "Display x 24", 24,
                "Caramelos", 7200, 60, false);
        producto("CHI-002", "Chicle globo tutti frutti", "Chicle para globos, sabor tutti frutti.", "Caja x 100",
                100, "Chicles", 4800, 110, false);
        producto("CHI-003", "Chicle sin azúcar", "Chicle de menta suave, endulzado con xilitol.", "Display x 20",
                20, "Chicles", 6100, 70, false);
        producto("CHO-002", "Chocolate amargo 70 %", "Tableta de 100 g con 70 % de cacao.", "Caja x 10", 10,
                "Chocolates", 11500, 45, false);
        producto("CHO-003", "Bombones surtidos", "Bombones rellenos de licor, dulce de leche y frutilla.",
                "Caja x 18", 18, "Chocolates", 9800, 35, true);
        producto("CHO-004", "Oblea bañada", "Oblea crocante bañada en chocolate con leche.", "Caja x 30", 30,
                "Chocolates", 8400, 90, false);
        producto("GOM-002", "Ositos de goma", "Ositos frutales de gelatina.", "Bolsa x 1 kg", 1, "Gomitas", 4600,
                130, false);
        producto("GOM-003", "Gomitas de frutilla", "Gomitas azucaradas con forma de frutilla.", "Bolsa x 1 kg", 1,
                "Gomitas", 4400, 100, false);
        producto("GOM-004", "Gusanitos ácidos", "Gusanitos bicolor cubiertos de azúcar ácida.", "Bolsa x 1 kg", 1,
                "Gomitas", 4700, 85, false);
        producto("GOM-005", "Malvaviscos", "Malvaviscos de vainilla, ideales para chocolate caliente.",
                "Bolsa x 500 g", 1, "Gomitas", 3300, 75, false);
    }

    private void producto(String codigo, String nombre, String descripcion, String presentacion, int unidades,
            String categoria, double precio, int stockInicial, boolean enOferta) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPresentacion(presentacion);
        producto.setUnidadesPorBulto(unidades);
        producto.setEnOferta(enOferta);
        producto.setCategoria(categoriaRepository.findAll().stream()
                .filter(c -> !c.isEliminado() && c.getNombre().equals(categoria)).findFirst().orElseThrow());
        producto.setImagen(imagenes.buscarEntidad(
                imagen("imagenes/demo/" + codigo.toLowerCase() + ".png", TipoImagen.PRODUCTO)));
        producto = productoRepository.save(producto);
        vigenciaService.crearVigenciaPrecio(producto.getId(), new VigenciaPrecioRequest(LocalDate.now(), precio));
        stock.reponer(producto.getId(), stockInicial, "Stock inicial de demostración");
    }

    /**
     * Dos precios anteriores por producto: 80 % del actual hasta hace 4 meses y 90 % hasta ayer. El precio de
     * hoy ya lo creó el service, así que las vigencias no se superponen.
     */
    private void cargarHistorialDePrecios() {
        LocalDate hoy = LocalDate.now();
        for (Producto producto : productoRepository.findByEliminadoFalse()) {
            double actual = precioActual(producto);
            vigencia(producto, hoy.minusDays(DIAS_DE_HISTORIA + 30), hoy.minusDays(DIAS_PRIMER_PRECIO + 1),
                    redondear(actual * 0.8));
            vigencia(producto, hoy.minusDays(DIAS_PRIMER_PRECIO), hoy.minusDays(1), redondear(actual * 0.9));
        }
    }

    private void vigencia(Producto producto, LocalDate desde, LocalDate hasta, double precio) {
        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setFechaDesde(desde);
        vigencia.setFechaHasta(hasta);
        vigencia.setPrecio(precio);
        vigenciaRepository.save(vigencia);
    }

    private double precioActual(Producto producto) {
        return vigenciaService.buscarPrecioVigente(producto.getId()).orElseThrow();
    }

    /** Precio del producto en una fecha pasada, según las vigencias de {@link #cargarHistorialDePrecios()}. */
    private double precioEn(Producto producto, LocalDate fecha) {
        double actual = precioActual(producto);
        return fecha.isBefore(LocalDate.now().minusDays(DIAS_PRIMER_PRECIO)) ? redondear(actual * 0.8)
                : redondear(actual * 0.9);
    }

    // ---------------------------------------------------------------- personas y comercios

    /** Dos preventores más, tres minoristas sin cuenta (los dio de alta el preventor) y sus comercios. */
    private void cargarPreventoresYComercios() {
        Usuario lucia = preventor("preventor2@dulcemayor.com.ar", "Lucía", "Ferreyra", "30000011", "P-002");
        Usuario tomas = preventor("preventor3@dulcemayor.com.ar", "Tomás", "Quiroga", "30000012", "P-003");

        String jose = minorista("José", "Rodríguez", "28111222");
        String carla = minorista("Carla", "Fernández", "29333444");
        String ana = minorista("Ana", "Sosa", "31555666");

        comercio("Despensa Don José", jose, "5501", "Tiburcio Benegas", "320", -32.9205, -68.8519, lucia);
        comercio("Kiosco El Trébol", jose, "5501", "Rivadavia", "75", -32.9330, -68.8402, lucia);
        comercio("Maxikiosco La Esquina", carla, "5539", "Independencia", "1500", -32.8580, -68.8350, lucia);
        comercio("Almacén Lo de Ana", ana, "5515", "Sarmiento", "210", -32.9845, -68.7905, tomas);
        comercio("Kiosco Plaza Maipú", ana, "5515", "Pablo Pescara", "90", -32.9810, -68.7830, tomas);
        comercio("Golosinería Luján", carla, "5507", "San Martín", "1100", -33.0368, -68.8790, tomas);
    }

    private Usuario preventor(String correo, String nombre, String apellido, String documento, String legajo) {
        Usuario usuario = usuarios.crearUsuario(correo, RolUsuario.PREVENTOR, DataSeeder.CLAVE_DEMO, false);
        Preventor preventor = new Preventor();
        preventor.setNombre(nombre);
        preventor.setApellido(apellido);
        preventor.setFechaNacimiento(LocalDate.of(1992, 3, 15));
        preventor.setTipoDocumento(TipoDocumento.DNI);
        preventor.setNumeroDocumento(documento);
        preventor.setLegajo(legajo);
        preventor.setUsuario(usuario);
        preventorRepository.save(preventor);
        return usuario;
    }

    private String minorista(String nombre, String apellido, String documento) {
        Minorista minorista = new Minorista();
        minorista.setNombre(nombre);
        minorista.setApellido(apellido);
        minorista.setFechaNacimiento(LocalDate.of(1980, 6, 20));
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento(documento);
        return minoristaRepository.save(minorista).getId();
    }

    private void comercio(String nombre, String minoristaId, String codigoPostal, String calle, String numero,
            double latitud, double longitud, Usuario preventor) {
        var localidad = localidadRepository.findByCodigoPostalAndEliminadoFalse(codigoPostal).orElseThrow();
        puntosService.crearPuntoDeVenta(new PuntoDeVentaRequest(nombre, minoristaId, localidad.getId(), calle,
                numero, null, null, null, "Comercio de demostración", latitud, longitud), preventor.getId());
    }

    // ---------------------------------------------------------------- ventas de los últimos meses

    /**
     * Seis meses de preventa (lunes a sábado) para los gráficos y los reportes. La mayoría de los pedidos se
     * entregó y se pagó; algunos se anularon (a veces después de quedar sin stock) y en la última semana
     * quedan facturas entregadas pendientes de pago. La actividad crece mes a mes.
     */
    private void cargarVentasHistoricas() {
        Usuario administrativo = usuarios.buscarUsuarioPorNombre("administrativo@dulcemayor.com.ar");
        Distribuidor distribuidor = distribuidorRepository.findByEliminadoFalse().get(0);
        List<Producto> productos = productoRepository.findByEliminadoFalse();
        List<FormaDePago> formas = formaDePagoRepository.findByEliminadoFalse();
        Map<String, List<PuntoDeVenta>> puntosPorPreventor = new HashMap<>();
        for (PuntoDeVenta punto : puntoRepository.findByEliminadoFalseOrderByNombreComercioAsc()) {
            puntosPorPreventor.computeIfAbsent(punto.getPreventor().getId(), id -> new ArrayList<>()).add(punto);
        }
        List<Preventor> preventores = preventorRepository.findByEliminadoFalse();
        long numeroFactura = facturaRepository.ultimoNumeroFactura();
        LocalDate hoy = LocalDate.now();

        for (int diasAtras = DIAS_DE_HISTORIA; diasAtras >= 1; diasAtras--) {
            LocalDate dia = hoy.minusDays(diasAtras);
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            double avance = 1 - (double) diasAtras / DIAS_DE_HISTORIA;
            for (Preventor preventor : preventores) {
                List<PuntoDeVenta> puntos = puntosPorPreventor.get(preventor.getId());
                if (puntos == null) continue;
                int cantidad = azar.nextDouble() < 0.35 + 0.4 * avance ? 1 + (azar.nextDouble() < 0.35 ? 1 : 0) : 0;
                for (int i = 0; i < cantidad; i++) {
                    PuntoDeVenta punto = puntos.get(azar.nextInt(puntos.size()));
                    LocalDateTime carga = dia.atTime(LocalTime.of(9 + azar.nextInt(8), azar.nextInt(60)));
                    Pedido pedido = pedidoHistorico(preventor, punto, productos, carga);
                    List<HistorialEstadoPedido> historial = new ArrayList<>();
                    historial.add(pedido.registrarCarga(preventor.getUsuario(), carga, null));

                    if (azar.nextDouble() < 0.07) {
                        if (azar.nextBoolean()) {
                            historial.add(pedido.avanzar(EventoPedido.MARCAR_SIN_STOCK, administrativo,
                                    carga.plusHours(2), null, null));
                        }
                        historial.add(pedido.avanzar(EventoPedido.ANULAR, administrativo, carga.plusDays(1),
                                "El comercio canceló el pedido.", null));
                        guardar(pedido, historial);
                        continue;
                    }

                    LocalDateTime facturado = carga.plusHours(3);
                    String numeroVisible = FacturacionService.numeroVisible(++numeroFactura);
                    historial.add(pedido.avanzar(EventoPedido.FACTURAR, administrativo, facturado,
                            "Factura N° " + numeroVisible + " emitida.", null));
                    LocalDateTime reparto = dia.plusDays(1).atTime(8, 30);
                    historial.add(pedido.avanzar(EventoPedido.INICIAR_REPARTO, distribuidor.getUsuario(), reparto,
                            null, distribuidor));
                    historial.add(pedido.avanzar(EventoPedido.ENTREGAR, distribuidor.getUsuario(),
                            reparto.plusHours(1 + azar.nextInt(6)), null, distribuidor));
                    guardar(pedido, historial);

                    boolean pagada = diasAtras > 7 || azar.nextDouble() < 0.6;
                    Factura factura = facturaHistorica(pedido, numeroFactura, facturado.toLocalDate(), pagada);
                    if (pagada) {
                        pagoHistorico(factura, formas.get(azar.nextInt(formas.size())),
                                facturado.toLocalDate().plusDays(azar.nextInt(4)));
                    }
                }
            }
        }
    }

    private Pedido pedidoHistorico(Preventor preventor, PuntoDeVenta punto, List<Producto> productos,
            LocalDateTime carga) {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(nuevoNumeroPedido());
        pedido.setPreventor(preventor);
        pedido.setPuntoDeVenta(punto);
        List<Producto> elegidos = new ArrayList<>(productos);
        java.util.Collections.shuffle(elegidos, azar);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Producto producto : elegidos.subList(0, 1 + azar.nextInt(4))) {
            int cantidad = 1 + azar.nextInt(10);
            double precio = precioEn(producto, carga.toLocalDate());
            BigDecimal importe = BigDecimal.valueOf(precio).multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(importe.doubleValue());
            pedido.agregarDetalle(detalle);
            subtotal = subtotal.add(importe);
        }
        pedido.setSubtotal(subtotal.doubleValue());
        pedido.setTotal(subtotal.doubleValue());
        return pedido;
    }

    private void guardar(Pedido pedido, List<HistorialEstadoPedido> historial) {
        pedidoRepository.save(pedido);
        historialRepository.saveAll(historial);
    }

    private Factura facturaHistorica(Pedido pedido, long numero, LocalDate fecha, boolean pagada) {
        Factura factura = new Factura();
        factura.setNumeroFactura(numero);
        factura.setFechaFactura(fecha);
        factura.setTotal(pedido.getTotal());
        factura.setEstado(pagada ? EstadoFactura.PAGADA : EstadoFactura.PENDIENTE_PAGO);
        factura.setPedido(pedido);
        for (DetallePedido linea : pedido.getDetalles()) {
            DetalleFactura detalle = new DetalleFactura();
            detalle.setProducto(linea.getProducto());
            detalle.setCantidad(linea.getCantidad());
            detalle.setPrecioUnitario(linea.getPrecioUnitario());
            detalle.setSubtotal(linea.getSubtotal());
            factura.agregarDetalle(detalle);
        }
        return facturaRepository.save(factura);
    }

    private void pagoHistorico(Factura factura, FormaDePago forma, LocalDate fecha) {
        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setFormaDePago(forma);
        pago.setMonto(factura.getTotal());
        pago.setMoneda(MonedaPago.ARS);
        pago.setFechaPago(fecha.isAfter(LocalDate.now()) ? LocalDate.now() : fecha);
        pago.setObservacion("Pago de demostración");
        pagoRepository.save(pago);
    }

    private long nuevoNumeroPedido() {
        long numero;
        do {
            numero = 100_000_000_000_000L + (long) (azar.nextDouble() * 899_999_999_999_999L);
        } while (!numerosPedido.add(numero));
        return numero;
    }

    // ---------------------------------------------------------------- pedidos de hoy (services reales)

    /**
     * Pedidos de hoy en todos los estados, cargados como desde las pantallas: dos CARGADO para facturar en
     * vivo, uno SIN_STOCK, uno FACTURADO con la factura pendiente de pago (el minorista de demo puede usar
     * «Pagar ahora»), uno FACTURADO y pagado listo para reparto y dos EN_REPARTO en la ruta del distribuidor.
     * Pedro (preventor de demo) solo visita hoy dos de sus cuatro comercios: su ruta arranca con puntos
     * pendientes y muestra el próximo más cercano (E6-01).
     */
    private void cargarPedidosDeHoy() {
        String pedro = usuarios.buscarUsuarioPorNombre("preventor@dulcemayor.com.ar").getId();
        String lucia = usuarios.buscarUsuarioPorNombre("preventor2@dulcemayor.com.ar").getId();
        String tomas = usuarios.buscarUsuarioPorNombre("preventor3@dulcemayor.com.ar").getId();
        String administrativo = usuarios.buscarUsuarioPorNombre("administrativo@dulcemayor.com.ar").getId();
        String distribuidor = usuarios.buscarUsuarioPorNombre("distribuidor@dulcemayor.com.ar").getId();
        String efectivo = formaDePagoRepository.findByEliminadoFalse().stream()
                .filter(f -> f.getTipoPago() == TipoPago.EFECTIVO).findFirst().orElseThrow().getId();

        // CARGADO (quedan para facturar durante la demo)
        pedido(pedro, "Kiosco Centro", Map.of("ALF-002", 4, "GOM-002", 6));
        pedido(lucia, "Despensa Don José", Map.of("CAR-002", 5, "CHI-002", 3));

        // SIN_STOCK: el segundo pedido de gomitas ácidas se queda sin stock al facturar el primero
        var sinStock = pedido(pedro, "Almacén Godoy Cruz", Map.of("GOM-001", 30));
        var primero = pedido(lucia, "Maxikiosco La Esquina", Map.of("GOM-001", 20, "ALF-004", 2));
        facturar(primero, administrativo);
        facturacion.facturarPedido(sinStock.id(), administrativo);

        // FACTURADO con factura pendiente de pago (minorista de demo)
        facturar(pedido(pedro, "Kiosco Centro", Map.of("CHO-004", 3, "CAR-003", 4)), administrativo);

        // FACTURADO y pagado: listo para reparto
        pagar(facturar(pedido(tomas, "Almacén Lo de Ana", Map.of("ALF-003", 2, "CHI-003", 2)), administrativo),
                efectivo);

        // EN_REPARTO: el distribuidor ya los tomó para su ruta de hoy
        var reparto1 = pedido(tomas, "Kiosco Plaza Maipú", Map.of("GOM-003", 5, "CAR-004", 1));
        var reparto2 = pedido(lucia, "Kiosco El Trébol", Map.of("CHO-002", 2, "GOM-004", 4));
        pagar(facturar(reparto1, administrativo), efectivo);
        pagar(facturar(reparto2, administrativo), efectivo);
        distribucion.tomarPedidos(distribuidor, new TomarPedidosRequest(List.of(reparto1.id(), reparto2.id())));
        pagar(primero.id(), efectivo);
    }

    private PedidoPreventaResponse pedido(String usuarioPreventor, String comercio, Map<String, Integer> lineas) {
        String puntoId = puntoRepository.findByEliminadoFalseOrderByNombreComercioAsc().stream()
                .filter(p -> p.getNombreComercio().equals(comercio)).findFirst().orElseThrow().getId();
        List<PedidoPreventaRequest.Linea> detalle = lineas.entrySet().stream()
                .map(e -> new PedidoPreventaRequest.Linea(
                        productoRepository.findByCodigoIgnoreCase(e.getKey()).orElseThrow().getId(), e.getValue()))
                .toList();
        return preventa.cargarPedido(new PedidoPreventaRequest(null, puntoId, null, detalle), usuarioPreventor);
    }

    private ResultadoFacturacionResponse facturar(PedidoPreventaResponse pedido, String usuario) {
        return facturacion.facturarPedido(pedido.id(), usuario);
    }

    private String pagar(ResultadoFacturacionResponse facturado, String formaDePagoId) {
        pagos.registrar(new PagoRequest(facturado.factura().id(), formaDePagoId, facturado.factura().total(),
                MonedaPago.ARS, "Pago en mostrador"));
        return facturado.pedidoId();
    }

    private void pagar(String pedidoId, String formaDePagoId) {
        var factura = facturacion.buscarFacturaPorPedido(pedidoId);
        pagos.registrar(new PagoRequest(factura.id(), formaDePagoId, factura.total(), MonedaPago.ARS,
                "Pago en mostrador"));
    }

    // ---------------------------------------------------------------- utilidades

    private String imagen(String ruta, TipoImagen tipo) {
        try {
            ClassPathResource recurso = new ClassPathResource(ruta);
            return imagenes.crearImagen(recurso.getFilename(), recurso.getContentAsByteArray(), tipo).id();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de demo " + ruta, ex);
        }
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
