package grupoing2.orm_ing2;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.List;

@Entity
public class DetalleFactura {
    
    @Id
    private Long id;
    
    private int cantidad;
    private int subtitulo;
    
    @OneToOne
    private Articulo articulo;
    
    @OneToOne
    private Factura factura;
    
    public Long getId() {
        return id;
    }
    
    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getSubtitulo() {
        return subtitulo;
    }

    public void setSubtitulo(int subtitulo) {
        this.subtitulo = subtitulo;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

}