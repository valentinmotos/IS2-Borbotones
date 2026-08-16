package grupoing2.orm_ing2;
import services_DAO.EstadoFacturaDAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import services_DAO.ClientesDAO;
import services_DAO.FacturaDAO;

public class ORM_ING2 {

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zeroPU");
        
        EntityManager em = emf.createEntityManager();
        
        EstadoFactura estadoFactura0 = new EstadoFactura();
        estadoFactura0.setNombre("Pagado");
        EstadoFactura estadoFactura1 = new EstadoFactura();
        estadoFactura1.setNombre("Espera Pago");
        EstadoFactura estadoFactura2 = new EstadoFactura();
        estadoFactura2.setNombre("Rechazado");
        
        EstadoFacturaDAO estadoFacturaDAO = new EstadoFacturaDAO( em );
        estadoFacturaDAO.crearFactura( estadoFactura0 );
        estadoFacturaDAO.crearFactura( estadoFactura1 );
        estadoFacturaDAO.crearFactura( estadoFactura2 );
        
        estadoFacturaDAO.listarEstadosFacturas();
        
        /* ------------------------------------*/
        
        Factura factura0 = new Factura();
        factura0.setNumeroFactura("99999");
        factura0.setFechaFactura("14/06/2000");
        factura0.setTotalPagado( 1550.0 );
        factura0.setEstadoFactura( estadoFactura0 );
        
        FacturaDAO facturaDAO = new FacturaDAO( em );
        facturaDAO.crearFactura( factura0 );
        
        /* ------------------------------------*/
        Cliente cliente1 = new Cliente();
        cliente1.setNroCliente( "42744458" );
        List<Factura> listFacturas = cliente1.getFacturas();
        listFacturas.add( factura0 );
        cliente1.setFacturas( listFacturas );
        
        Cliente cliente2 = new Cliente();
        cliente2.setNroCliente( "15551146" );
        
        ClientesDAO clientesDAO = new ClientesDAO( em );
        clientesDAO.crearCliente(cliente1);
        clientesDAO.crearCliente(cliente2);
        
        /* ------------------------------------*/
        
        List<Cliente> listClientes = clientesDAO.listarClientes();
        for (Cliente cliente : listClientes) {
            System.out.println( "Cliente -->" );
            System.out.println( cliente.getNroCliente() );
            List<Factura> listFacturasCliente = cliente.getFacturas();
            for (Factura facturaCliente : listFacturasCliente) {
                System.out.println( "- -Factura -->>" );
                System.out.println( facturaCliente.getNumeroFactura() );
                System.out.println( "- ----------->>" );
            }
            System.out.println( "---------->" );
            
        }
        
        em.close();
        
        emf.close();
        
        System.out.println("JPA funciona correctamente");
        
        emf.close();
    }
}