package com.borbotones.integrador1.services;

import com.borbotones.integrador1.dto.OrdenCompraDAO;
import com.borbotones.integrador1.entities.Cliente;
import com.borbotones.integrador1.entities.DetalleCompra;
import com.borbotones.integrador1.entities.OrdenCompra;
import com.borbotones.integrador1.entities.Producto;

public class OrdenCompraServicie {

    public boolean existeProductoEnOrdenCompra( String idOrdenCompra , String idCliente , String idProducto ){
        try{
            OrdenCompraDAO objOrdenCompraDAO = new OrdenCompraDAO()
            OrdenCompra ordenCompra = objOrdenCompraDAO.buscarOrdenCompra( idOrdenCompra );

            for (DetalleCompra detalleCompra : ordenCompra.getDetallesCompra()) {
                if (!detalleCompra.isEliminado() && detalleCompra.getProducto().getId().equals(idProducto)) {
                    return true;
                }
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    public void addProductoOrdenCompra( String idCliente , String idOrdenCompra , String idProducto ){
        //Se valida si existe una OrdenCompra, sino se crea
        //Se agrega DetalleCompra con cantidad=0
        try{
            if ( !this.existeProductoEnOrdenCompra ( idOrdenCompra , idProducto ) ){
                Producto producto = objProductoDAO.buscarProducto( idProducto );

                objOrdenCompraDAO.addDetalleProducto( ordenCompra, producto );
            }
        }catch (Exception e){

        }
    }

    public void modificarProductoOrdenCompra( String idCliente , String idProducto , int cantidad ){
        //Si cantidad=0 se elimina
        //Se valida
        try{

        }catch (Exception e){

        }
    }

}
