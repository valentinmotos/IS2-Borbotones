package com.tp2.servidor.template;

import com.tp2.servidor.dao.CarritoDao;
import com.tp2.servidor.dto.CrearCarritoRequest;
import com.tp2.servidor.entity.Articulo;
import com.tp2.servidor.entity.Carrito;
import com.tp2.servidor.entity.Detalle;
import com.tp2.servidor.service.BaseRestService;

/** Template Method para los casos de uso particulares del carrito. */
public abstract class CarritoTemplate extends BaseRestService<Carrito, CrearCarritoRequest, String> {
    private final CarritoDao carritoDao;

    protected CarritoTemplate(CarritoDao carritoDao, UsuarioServiceLookup lookup) {
        super(carritoDao, request -> {
            Carrito carrito = new Carrito();
            carrito.setUsuario(lookup.buscar(request.getUsuarioId()));
            return carrito;
        }, carrito -> !carrito.isEliminado());
        this.carritoDao = carritoDao;
    }

    public final Carrito agregarArticulo(String carritoId, Articulo articulo, int cantidad) {
        validarCantidad(cantidad);
        Carrito carrito = obtenerCarritoActivo(carritoId);
        validarCarritoEditable(carrito);
        agregarOIncrementarDetalle(carrito, articulo, cantidad);
        recalcularTotal(carrito);
        return carritoDao.save(carrito);
    }

    public final Carrito editarDetalle(String carritoId, String detalleId, int cantidad) {
        Carrito carrito = obtenerCarritoActivo(carritoId);
        validarCarritoEditable(carrito);
        modificarDetalle(carrito, detalleId, cantidad);
        recalcularTotal(carrito);
        return carritoDao.save(carrito);
    }

    public final Carrito finalizarCompra(String carritoId) {
        Carrito carrito = obtenerCarritoActivo(carritoId);
        validarCarritoEditable(carrito);
        validarCarritoConArticulos(carrito);
        carrito.setFinalizado(true);
        recalcularTotal(carrito);
        return carritoDao.save(carrito);
    }

    @Override
    protected void marcarEliminada(Carrito carrito) {
        carrito.setEliminado(true);
        carrito.getDetalles().forEach(detalle -> detalle.setEliminado(true));
    }

    protected Carrito obtenerCarritoActivo(String id) {
        return carritoDao.findById(id).filter(carrito -> !carrito.isEliminado())
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado o eliminado"));
    }

    protected void validarCantidad(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
    }

    protected void validarCarritoEditable(Carrito carrito) {
        if (carrito.isFinalizado()) throw new IllegalArgumentException("El carrito ya fue finalizado");
    }

    protected void agregarOIncrementarDetalle(Carrito carrito, Articulo articulo, int cantidad) {
        Detalle existente = carrito.getDetalles().stream()
                .filter(detalle -> !detalle.isEliminado())
                .filter(detalle -> detalle.getArticulo().getId().equals(articulo.getId()))
                .findFirst().orElse(null);
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            return;
        }
        Detalle detalle = new Detalle();
        detalle.setCarrito(carrito);
        detalle.setArticulo(articulo);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(articulo.getPrecio());
        carrito.getDetalles().add(detalle);
    }

    protected void modificarDetalle(Carrito carrito, String detalleId, int cantidad) {
        Detalle detalle = carrito.getDetalles().stream().filter(item -> item.getId().equals(detalleId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado"));
        if (cantidad <= 0) detalle.setEliminado(true);
        else detalle.setCantidad(cantidad);
    }

    protected void validarCarritoConArticulos(Carrito carrito) {
        if (carrito.getDetalles().stream().noneMatch(d -> !d.isEliminado() && d.getCantidad() > 0))
            throw new IllegalArgumentException("El carrito no tiene articulos");
    }

    protected void recalcularTotal(Carrito carrito) {
        carrito.setTotal(carrito.getDetalles().stream().filter(d -> !d.isEliminado())
                .mapToDouble(d -> d.getCantidad() * d.getPrecioUnitario()).sum());
    }

    @FunctionalInterface
    public interface UsuarioServiceLookup {
        com.tp2.servidor.entity.Usuario buscar(String usuarioId);
    }
}
