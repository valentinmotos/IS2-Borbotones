package com.dulcemayor.controller;

import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.dto.PedidoPreventaRequest;
import com.dulcemayor.dto.PedidoPreventaResponse;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.PreventaService;
import com.dulcemayor.service.PreventorService;
import com.dulcemayor.service.PuntoDeVentaService;
import com.dulcemayor.service.RegistroMinoristaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preventa")
public class PreventaController {
    private final PreventaService preventa;
    private final PreventorService preventores;
    private final PuntoDeVentaService puntos;
    private final RegistroMinoristaService registro;

    public PreventaController(PreventaService preventa, PreventorService preventores,
            PuntoDeVentaService puntos, RegistroMinoristaService registro) {
        this.preventa = preventa;
        this.preventores = preventores;
        this.puntos = puntos;
        this.registro = registro;
    }

    @GetMapping("/puntos-de-venta")
    public List<PuntoDeVentaResponse> puntos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return puntos.listarPuntoDeVentaPorPreventor(preventores.buscarPreventorPorUsuario(usuario.id()).getId());
    }

    @PostMapping("/minoristas")
    @ResponseStatus(HttpStatus.CREATED)
    public MinoristaOpcion registrarMinorista(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody RegistroMinoristaRequest request) {
        preventores.buscarPreventorPorUsuario(usuario.id());
        return registro.registrarPerfil(request);
    }

    @GetMapping("/pedidos")
    public List<PedidoPreventaResponse> pedidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return preventa.listarPedidos(usuario.id());
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoPreventaResponse cargar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PedidoPreventaRequest request) {
        return preventa.cargarPedido(request, usuario.id());
    }
}
