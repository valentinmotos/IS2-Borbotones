package com.borbotones.integrador1.services;

import com.borbotones.integrador1.entities.Cliente;
import com.borbotones.integrador1.entities.Producto;

public class OrdenCompraServicie {

    public boolean existeProductoEnOrdenCompra( String idCliente , String idProducto ){
        try{

            return true;
        }catch (Exception e){
            return false;
        }
    }

    public void addProductoOrdenCompra( String idCliente , String idProducto ){
        //Se valida si existe una OrdenCompra, sino se crea
        //Se agrega DetalleCompra con cantidad=0
        try{

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
