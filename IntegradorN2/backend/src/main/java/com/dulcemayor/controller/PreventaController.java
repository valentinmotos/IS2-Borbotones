package com.dulcemayor.controller;

import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.dto.PedidoPreventaRequest;
import com.dulcemayor.dto.PedidoPreventaResponse;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.dto.RutaResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.PreventaService;
import com.dulcemayor.service.PreventorService;
import com.dulcemayor.service.PuntoDeVentaService;
import com.dulcemayor.service.RegistroMinoristaService;
import com.dulcemayor.service.RutaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preventa")
public class PreventaController {
    private final PreventaService preventa;
    private final PreventorService preventores;
    private final PuntoDeVentaService puntos;
    private final RegistroMinoristaService registro;
    private final RutaService rutas;

    public PreventaController(PreventaService preventa, PreventorService preventores,
            PuntoDeVentaService puntos, RegistroMinoristaService registro, RutaService rutas) {
        this.preventa = preventa;
        this.preventores = preventores;
        this.puntos = puntos;
        this.registro = registro;
        this.rutas = rutas;
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

    /**
     * E5-01: ruta de hoy del preventor autenticado con el próximo punto más cercano y el recorrido
     * sugerido. La posición (GPS del celular) es opcional; sin ella se parte del último punto visitado.
     */
    @GetMapping("/ruta")
    public RutaResponse ruta(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Double latitud, @RequestParam(required = false) Double longitud) {
        return rutas.obtenerRutaDelDia(usuario.id(), latitud, longitud);
    }
}
