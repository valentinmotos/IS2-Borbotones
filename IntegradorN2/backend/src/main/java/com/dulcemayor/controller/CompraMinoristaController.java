package com.dulcemayor.controller;

import com.dulcemayor.dto.ArchivoDescargable;
import com.dulcemayor.dto.CompraMinoristaDetalleResponse;
import com.dulcemayor.dto.CompraMinoristaResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.CompraMinoristaService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * E4-08: compras y facturas del minorista autenticado. Solo MINORISTA (regla de /api/minorista/** en
 * SecurityConfig). No recibe el id del minorista: sale del token. Un pedido o una factura de otro
 * minorista responde 404. El pago con billetera usa el checkout de E4-05
 * ({@code POST /api/minorista/facturas/{facturaId}/checkout}).
 */
@RestController
@RequestMapping("/api/minorista")
public class CompraMinoristaController {
    private final CompraMinoristaService compras;

    public CompraMinoristaController(CompraMinoristaService compras) {
        this.compras = compras;
    }

    @GetMapping("/compras")
    public List<CompraMinoristaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return compras.listarPedidoPorMinorista(usuario.id());
    }

    @GetMapping("/compras/{pedidoId}")
    public CompraMinoristaDetalleResponse buscar(@PathVariable String pedidoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return compras.buscarPedidoDelMinorista(usuario.id(), pedidoId);
    }

    @GetMapping("/facturas/{facturaId}/pdf")
    public ResponseEntity<byte[]> descargarFactura(@PathVariable String facturaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ArchivoDescargable archivo = compras.descargarFacturaDelMinorista(usuario.id(), facturaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }
}
