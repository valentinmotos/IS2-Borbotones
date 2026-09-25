package com.zero.ecommerce.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.zero.ecommerce.dto.FilaTablaDTO;

@Controller
public class DevController {

    @GetMapping("/dev/ejemplo-publico")
    public String ejemploPublico() {
        return "dev/ejemplo-publico";
    }

    @GetMapping("/dev/ejemplo-admin")
    public String ejemploAdmin() {
        return "dev/ejemplo-admin";
    }

    @GetMapping("/dev/componentes")
    public String componentes(Model model) {
        model.addAttribute("exito", "La operación se realizó correctamente.");
        model.addAttribute("error", "Este es un mensaje de validación de ejemplo.");
        model.addAttribute("encabezados", List.of("Producto", "Estado", "Precio"));
        model.addAttribute("filas", List.of(
            List.of("Remera Zero Pro", "Publicado", "$12.500"),
            List.of("Short Zero Move", "Pendiente", "$9.900")));
        model.addAttribute("filasVacias", List.of());
        model.addAttribute("encabezadosRegistros", List.of("Tipo de pago", "Observación"));
        model.addAttribute("registros", List.of(
            new FilaTablaDTO("demo-1", "Mercado Pago", List.of("Billetera virtual", "Mercado Pago")),
            new FilaTablaDTO("demo-2", "Banco Nación", List.of("Transferencia", "Banco Nación"))));
        Map<String, String> opciones = new LinkedHashMap<>();
        opciones.put("EFECTIVO", "Efectivo");
        opciones.put("TRANSFERENCIA", "Transferencia");
        opciones.put("BILLETERA_VIRTUAL", "Billetera virtual");
        model.addAttribute("opciones", opciones);
        return "dev/componentes";
    }
}
