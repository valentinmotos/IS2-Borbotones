package com.zero.ecommerce.controllers.publico;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zero.ecommerce.dto.LocalidadDTO;
import com.zero.ecommerce.dto.UbicacionDTO;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

/**
 * Endpoints JSON públicos para los selects en cascada de fragments/direccion.
 * Devuelven solo registros activos; un id vacío o inexistente devuelve una lista vacía.
 */
@RestController
@RequestMapping("/api/ubicacion")
public class UbicacionApiController {

    private final PaisService paisService;
    private final ProvinciaService provinciaService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;

    public UbicacionApiController(PaisService paisService, ProvinciaService provinciaService,
            DepartamentoService departamentoService, LocalidadService localidadService) {
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
    }

    @GetMapping("/paises")
    public List<UbicacionDTO> paises() {
        return paisService.listarPaisActivo().stream()
                .map(p -> new UbicacionDTO(p.getId(), p.getNombre()))
                .toList();
    }

    @GetMapping("/provincias")
    public List<UbicacionDTO> provincias(@RequestParam String pais) {
        if (pais.isBlank()) {
            return List.of();
        }
        return provinciaService.listarProvinciaActivo(pais).stream()
                .map(p -> new UbicacionDTO(p.getId(), p.getNombre()))
                .toList();
    }

    @GetMapping("/departamentos")
    public List<UbicacionDTO> departamentos(@RequestParam String provincia) {
        if (provincia.isBlank()) {
            return List.of();
        }
        return departamentoService.listarDepartamentoActivo(provincia).stream()
                .map(d -> new UbicacionDTO(d.getId(), d.getNombre()))
                .toList();
    }

    @GetMapping("/localidades")
    public List<LocalidadDTO> localidades(@RequestParam String departamento) {
        if (departamento.isBlank()) {
            return List.of();
        }
        return localidadService.listarLocalidadActivo(departamento).stream()
                .map(l -> new LocalidadDTO(l.getId(), l.getNombre(), l.getCodigoPostal()))
                .toList();
    }
}
