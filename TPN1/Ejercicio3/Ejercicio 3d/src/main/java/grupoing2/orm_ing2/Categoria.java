package grupoing2.orm_ing2;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;

@Entity
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String denominacion;

    @OneToMany
    private List<Articulo> articulos;

    public Long getId() {
        return id;
    }
    
    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}