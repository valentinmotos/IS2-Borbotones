
package grupoing2.orm_ing2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
public class EstadoFactura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    public Long getId(){
        return id;
    }
    
    public EstadoFactura(){}
    
    public String getNombre(){
        return nombre;
    }
    
    public void setNombre( String nombre ){
        this.nombre = nombre;
    }
}
