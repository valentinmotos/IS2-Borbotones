package grupoing2.orm_ing2;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
    @Index(name = "idx_articulo_denominacion", columnList = "denominacion")
})
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int cantidad;
    private String denominacion;
    private int precio;

    @OneToMany
    private List<Categoria> categorias;

    @OneToMany
    private List<DetalleFactura> detallesFacturas;

    public Articulo() {
    }

    public Articulo(int cantidad, String denominacion, int precio) {
        this.cantidad = cantidad;
        this.denominacion = denominacion;
        this.precio = precio;
    }

    public Long getId() {
        return id;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public List<Categoria> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<Categoria> categorias) {
        this.categorias = categorias;
    }

    public List<DetalleFactura> getDetallesFactura() {
        return detallesFacturas;
    }

    public void setDetallesFactura(List<DetalleFactura> detallesFactura) {
        this.detallesFacturas = detallesFactura;
    }

}