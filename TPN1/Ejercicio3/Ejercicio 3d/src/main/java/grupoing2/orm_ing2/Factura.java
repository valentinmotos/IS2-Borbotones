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
public class Factura {
    
    @Id
    private Long id;
    
    private String fecha;
    private int numero;
    private int total;
    
    @OneToMany
    private List<DetalleFactura> detallesFacturas;
    
    @OneToOne
    private Cliente cliente;
    
    public Long getId() {
        return id;
    }
    
    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<DetalleFactura> getDetallesFacturas() {
        return detallesFacturas;
    }

    public void setDetallesFacturas(List<DetalleFactura> detallesFacturas) {
        this.detallesFacturas = detallesFacturas;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}