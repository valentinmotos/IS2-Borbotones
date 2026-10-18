package com.tp2.servidor.config;

import com.tp2.servidor.dto.ArticuloRequest;
import com.tp2.servidor.dto.ImagenRequest;
import com.tp2.servidor.dto.ProveedorRequest;
import com.tp2.servidor.dto.UsuarioRequest;
import com.tp2.servidor.entity.Proveedor;
import com.tp2.servidor.service.ArticuloService;
import com.tp2.servidor.service.ImagenService;
import com.tp2.servidor.service.ProveedorService;
import com.tp2.servidor.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioService usuarioService;
    private final ProveedorService proveedorService;
    private final ImagenService imagenService;
    private final ArticuloService articuloService;

    public DataInitializer(UsuarioService usuarioService, ProveedorService proveedorService,
                           ImagenService imagenService, ArticuloService articuloService) {
        this.usuarioService = usuarioService;
        this.proveedorService = proveedorService;
        this.imagenService = imagenService;
        this.articuloService = articuloService;
    }

    @Override
    public void run(String... args) {
        if (usuarioService.listar().isEmpty()) {
            UsuarioRequest usuario = new UsuarioRequest();
            usuario.setNombre("Cliente Demo");
            usuario.setClave("1234");
            usuarioService.crear(usuario);
        }

        if (articuloService.listar().isEmpty()) {
            Proveedor tecno = crearProveedor("Tecno Insumos");
            Proveedor pantallas = crearProveedor("Pantallas del Oeste");
            crearArticulo("Teclado mecanico", 45000, "#2f80ed", tecno);
            crearArticulo("Mouse optico", 18000, "#27ae60", tecno);
            crearArticulo("Monitor 24 pulgadas", 135000, "#eb5757", pantallas);
        }
    }

    private Proveedor crearProveedor(String nombre) {
        ProveedorRequest proveedor = new ProveedorRequest();
        proveedor.setNombre(nombre);
        return proveedorService.crear(proveedor);
    }

    private void crearArticulo(String nombre, double precio, String color, Proveedor proveedor) {
        ImagenRequest imagen = new ImagenRequest();
        imagen.setNombre(nombre + ".svg");
        imagen.setMime("image/svg+xml");
        imagen.setContenido(svg(nombre, color));

        ArticuloRequest articulo = new ArticuloRequest();
        articulo.setNombre(nombre);
        articulo.setPrecio(precio);
        articulo.setImagenId(imagenService.crear(imagen).getId());
        articulo.setProveedorId(proveedor.getId());
        articuloService.crear(articulo);
    }

    private byte[] svg(String texto, String color) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"450\" height=\"300\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"" + color + "\"/>"
                + "<text x=\"50%\" y=\"50%\" fill=\"#ffffff\" font-family=\"Arial\" font-size=\"28\" "
                + "text-anchor=\"middle\" dominant-baseline=\"middle\">" + texto + "</text></svg>";
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
