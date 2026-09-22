package com.borbotones.integrador1.controllers;

import com.borbotones.integrador1.services.OrdenCompraServicie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ordenCompraController {

    @GetMapping("/ordenCompra/{idCliente}")
    public String inicio( @PathVariable String idCliente, Model model) {
        return "pasarelaPagoOrdenCompra";
    }

    @PostMapping({"/modificarProductoOrdenCompra"})
    public String modificarProductoOrdenCompra( @RequestParam String idProducto,
                                                @RequestParam String idCliente,
                                                @RequestParam int cantidad, Model model ) {
        try{
            OrdenCompraServicie objOrdenCompraService = new OrdenCompraServicie();
            boolean existeProducto = objOrdenCompraService.existeProductoEnOrdenCompra( idCliente, idProducto );
            if ( existeProducto == false ) {
                objOrdenCompraService.addProductoOrdenCompra( idProducto, idCliente);
            }
            objOrdenCompraService.modificarProductoOrdenCompra( idProducto, idCliente, cantidad);
            return "pasarelaPagoOrdenCompra";
        }catch (Exception e){
            return "pasarelaPagoOrdenCompra";
        }

    }

}
