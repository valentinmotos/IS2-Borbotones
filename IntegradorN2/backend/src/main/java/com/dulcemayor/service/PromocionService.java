package com.dulcemayor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.PromocionRequest;
import com.dulcemayor.dto.PromocionResponse;
import com.dulcemayor.dto.mapper.PromocionMapper;
import com.dulcemayor.entity.DetallePedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Promocion;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.PromocionRepository;

/** E4-03: ABM y aplicación de promociones por volumen o producto destacado. */
@Service
@Transactional(readOnly = true)
public class PromocionService extends AbstractCrudService<Promocion, PromocionRequest, PromocionResponse> {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final String PROMOCION_NO_VIGENTE = "El código de promoción no existe o está vencido.";

    private final PromocionRepository promociones;
    private final ProductoRepository productos;

    public PromocionService(PromocionRepository promociones, ProductoRepository productos, PromocionMapper mapper) {
        super(promociones, mapper);
        this.promociones = promociones;
        this.productos = productos;
    }

    /** Ordena las promociones activas por código. */
    public List<PromocionResponse> listarPromociones() {
        return mapper.aRespuestas(promociones.findByEliminadoFalseOrderByCodigoAsc());
    }

    /** Obtiene un código únicamente si está dentro de sus fechas de vigencia. */
    public Promocion buscarPromocionVigentePorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ErrorServiceException("Ingresá un código de promoción.", "codigo");
        }
        LocalDate hoy = LocalDate.now();
        return promociones
                .findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                        normalizarCodigo(codigo), hoy, hoy)
                .orElseThrow(() -> new ErrorServiceException(PROMOCION_NO_VIGENTE, "codigo"));
    }

    /**
     * Aplica el código sobre un pedido antes de guardarlo. Conserva el subtotal bruto, registra el
     * descuento y actualiza el total. Sin código, deja el pedido sin descuento.
     *
     * @return descuento calculado en pesos
     */
    @Transactional
    public double aplicarPromocion(Pedido pedido, String codigo) {
        if (pedido == null) {
            throw new ErrorServiceException("El pedido es obligatorio.");
        }
        double subtotal = pedido.getSubtotal();
        if (!Double.isFinite(subtotal) || subtotal < 0) {
            throw new ErrorServiceException("El subtotal del pedido no es válido.", "subtotal");
        }
        if (codigo == null || codigo.isBlank()) {
            pedido.setCodigoPromocion(null);
            pedido.setDescuento(0);
            pedido.setTotal(subtotal);
            return 0;
        }

        Promocion promocion = buscarPromocionVigentePorCodigo(codigo);
        double baseDescuento;
        if (promocion.getCantidadMinima() != null) {
            long cantidadTotal = pedido.getDetalles().stream().mapToLong(DetallePedido::getCantidad).sum();
            if (cantidadTotal < promocion.getCantidadMinima()) {
                throw new ErrorServiceException("El pedido no alcanza la cantidad mínima de "
                        + promocion.getCantidadMinima() + " bultos para este código.", "codigo");
            }
            baseDescuento = subtotal;
        } else {
            String productoPromocionado = promocion.getProducto().getId();
            baseDescuento = pedido.getDetalles().stream()
                    .filter(detalle -> detalle.getProducto() != null
                            && productoPromocionado.equals(detalle.getProducto().getId()))
                    .mapToDouble(DetallePedido::getSubtotal)
                    .sum();
            if (baseDescuento <= 0) {
                throw new ErrorServiceException("El pedido no contiene el producto incluido en esta promoción.",
                        "codigo");
            }
        }

        double descuento = BigDecimal.valueOf(baseDescuento)
                .multiply(BigDecimal.valueOf(promocion.getPorcentajeDescuento()))
                .divide(CIEN, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(subtotal))
                .doubleValue();
        pedido.setCodigoPromocion(promocion.getCodigo());
        pedido.setDescuento(descuento);
        pedido.setTotal(BigDecimal.valueOf(subtotal).subtract(BigDecimal.valueOf(descuento))
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        return descuento;
    }

    @Override
    protected void validar(PromocionRequest request, String idActual) {
        if (request == null) {
            throw new ErrorServiceException("Los datos de la promoción son obligatorios.");
        }
        if (request.codigo() == null || request.codigo().isBlank()) {
            throw new ErrorServiceException("El código es obligatorio.", "codigo");
        }
        String codigo = normalizarCodigo(request.codigo());
        boolean duplicado = idActual == null
                ? promociones.existsByCodigoIgnoreCase(codigo)
                : promociones.existsByCodigoIgnoreCaseAndIdNot(codigo, idActual);
        if (duplicado) {
            throw new DuplicadoException("Ya existe una promoción con ese código.", "codigo");
        }
        if (request.descripcion() == null || request.descripcion().isBlank()) {
            throw new ErrorServiceException("La descripción es obligatoria.", "descripcion");
        }
        if (request.porcentajeDescuento() == null || !Double.isFinite(request.porcentajeDescuento())
                || request.porcentajeDescuento() <= 0 || request.porcentajeDescuento() > 100) {
            throw new ErrorServiceException("El porcentaje debe ser mayor que cero y no superar el 100%.",
                    "porcentajeDescuento");
        }
        if (request.fechaDesde() == null || request.fechaHasta() == null) {
            throw new ErrorServiceException("Ingresá las fechas desde y hasta.", "fechaDesde");
        }
        if (request.fechaHasta().isBefore(request.fechaDesde())) {
            throw new ErrorServiceException("La fecha hasta debe ser igual o posterior a la fecha desde.",
                    "fechaHasta");
        }

        boolean porVolumen = request.cantidadMinima() != null;
        boolean porProducto = request.idProducto() != null && !request.idProducto().isBlank();
        if (porVolumen == porProducto) {
            throw new ErrorServiceException("La promoción debe ser por volumen o por producto, no por ambos.",
                    "cantidadMinima");
        }
        if (porVolumen && request.cantidadMinima() <= 0) {
            throw new ErrorServiceException("La cantidad mínima debe ser mayor que cero.", "cantidadMinima");
        }
        if (porProducto) productoActivo(request.idProducto());
    }

    @Override
    protected Promocion nuevaEntidad() {
        return new Promocion();
    }

    @Override
    protected void asignar(Promocion promocion, PromocionRequest request) {
        super.asignar(promocion, request);
        promocion.setCodigo(normalizarCodigo(request.codigo()));
        promocion.setDescripcion(request.descripcion().strip());
        promocion.setProducto(request.idProducto() == null || request.idProducto().isBlank()
                ? null
                : productoActivo(request.idProducto()));
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "La promoción no existe o fue dada de baja.";
    }

    @Override
    protected Sort orden() {
        return Sort.by("codigo");
    }

    private Producto productoActivo(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException("No se encontró el producto de la promoción.");
        }
        return productos.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto de la promoción."));
    }

    private String normalizarCodigo(String codigo) {
        return codigo.strip().toUpperCase(Locale.ROOT);
    }
}
