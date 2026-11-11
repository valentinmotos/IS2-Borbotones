package com.dulcemayor.config;

import java.time.LocalDate;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.dto.DepartamentoRequest;
import com.dulcemayor.dto.DepartamentoResponse;
import com.dulcemayor.dto.FormaDePagoRequest;
import com.dulcemayor.dto.LocalidadRequest;
import com.dulcemayor.dto.MayoristaRequest;
import com.dulcemayor.dto.PerfilMinoristaRequest;
import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.ProductoRequest;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import com.dulcemayor.entity.enums.TipoEmpresa;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.service.CategoriaService;
import com.dulcemayor.service.ProductoService;
import com.dulcemayor.service.StockService;
import com.dulcemayor.service.VigenciaPrecioService;
import com.dulcemayor.service.DepartamentoService;
import com.dulcemayor.service.FormaDePagoService;
import com.dulcemayor.service.ImagenService;
import com.dulcemayor.service.LocalidadService;
import com.dulcemayor.service.MayoristaService;
import com.dulcemayor.service.SucursalService;
import com.dulcemayor.service.UsuarioService;
import com.dulcemayor.service.MinoristaService;
import com.dulcemayor.service.PuntoDeVentaService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.web.multipart.MultipartFile;

/** Carga los datos iniciales solo cuando la base está vacía y a través de los services. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String CLAVE_DEMO = "Demo1234!";

    private final EntityManager entityManager;
    private final CategoriaService categoriaService;
    private final ProductoService productoService;
    private final StockService stockService;
    private final VigenciaPrecioService vigenciaPrecioService;
    private final UsuarioService usuarioService;
    private final ImagenService imagenService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;
    private final MayoristaService mayoristaService;
    private final SucursalService sucursalService;
    private final FormaDePagoService formaDePagoService;
    private final LocalidadRepository localidadRepository;
    private final PuntoDeVentaService puntoDeVentaService;
    private final PreventorRepository preventorRepository;
    private final MinoristaRepository minoristaRepository;
    private final MinoristaService minoristaService;

    public DataSeeder(EntityManager entityManager, CategoriaService categoriaService,
            ProductoService productoService, StockService stockService, VigenciaPrecioService vigenciaPrecioService,
            UsuarioService usuarioService, ImagenService imagenService,
            DepartamentoService departamentoService, LocalidadService localidadService,
            MayoristaService mayoristaService, SucursalService sucursalService,
            FormaDePagoService formaDePagoService, LocalidadRepository localidadRepository,
            PuntoDeVentaService puntoDeVentaService, PreventorRepository preventorRepository,
            MinoristaRepository minoristaRepository, MinoristaService minoristaService) {
        this.entityManager = entityManager;
        this.categoriaService = categoriaService;
        this.productoService = productoService;
        this.stockService = stockService;
        this.vigenciaPrecioService = vigenciaPrecioService;
        this.usuarioService = usuarioService;
        this.imagenService = imagenService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
        this.mayoristaService = mayoristaService;
        this.sucursalService = sucursalService;
        this.formaDePagoService = formaDePagoService;
        this.localidadRepository = localidadRepository;
        this.puntoDeVentaService = puntoDeVentaService;
        this.preventorRepository = preventorRepository;
        this.minoristaRepository = minoristaRepository;
        this.minoristaService = minoristaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!baseVacia()) {
            log.info("La base ya tiene datos: no se ejecuta el seeder.");
            cargarCatalogo();
            return;
        }
        log.info("Base vacía: cargando datos iniciales.");
        cargarUbicacion();
        cargarEmpresa();
        cargarFormasDePago();
        cargarUsuarios();
        cargarCatalogo();
        cargarPuntosDeVenta();
        cargarPedidos();
    }

    /** E1-03: departamentos y localidades de Mendoza. */
    private void cargarUbicacion() {
        String[][] datos = {
                { "Capital", "Ciudad de Mendoza", "5500" },
                { "Godoy Cruz", "Godoy Cruz", "5501" },
                { "Guaymallén", "Villa Nueva", "5521" },
                { "Las Heras", "Las Heras", "5539" },
                { "Luján de Cuyo", "Luján de Cuyo", "5507" },
                { "Maipú", "Maipú", "5515" }
        };
        for (String[] dato : datos) {
            DepartamentoResponse departamento = departamentoService
                    .crearDepartamento(new DepartamentoRequest(dato[0]));
            localidadService.crearLocalidad(new LocalidadRequest(dato[1], dato[2], departamento.id()));
        }
    }

    /** E1-07: mayorista y sede central de demostración. */
    private void cargarEmpresa() {
        var empresa = mayoristaService.crearMayorista(new MayoristaRequest("Dulce Mayor S.A.", "30-70000000-8"));
        var localidad = localidadRepository.findByEliminadoFalse(Sort.by("nombre")).get(0);
        String imagenId = cargarImagen("imagenes/imagen-prueba.png", TipoImagen.SUCURSAL);
        sucursalService.crearSucursal(new SucursalRequest("Casa Central", TipoEmpresa.SEDE_CENTRAL,
                empresa.id(), localidad.getId(), "San Martín", "1234", "Centro", null, null,
                "Sucursal de demostración", -32.8895, -68.8458, imagenId));
    }

    /** E1-07: formas de pago habilitadas inicialmente. */
    private void cargarFormasDePago() {
        formaDePagoService.crearFormaDePago(new FormaDePagoRequest(TipoPago.EFECTIVO, null));
        formaDePagoService.crearFormaDePago(new FormaDePagoRequest(TipoPago.TRANSFERENCIA, "dulcemayor.mp"));
        formaDePagoService.crearFormaDePago(new FormaDePagoRequest(TipoPago.BILLETERA_VIRTUAL, "Mercado Pago"));
    }

    /** E1-01: usuarios de demo de cada rol. */
    private void cargarUsuarios() {
        usuarioService.crearUsuario("jefe@dulcemayor.com.ar", RolUsuario.JEFE, CLAVE_DEMO, false);
        usuarioService.crearUsuario("administrativo@dulcemayor.com.ar", RolUsuario.ADMINISTRATIVO, CLAVE_DEMO, false);
        usuarioService.crearUsuario("preventor@dulcemayor.com.ar", RolUsuario.PREVENTOR, CLAVE_DEMO, false);
        usuarioService.crearUsuario("distribuidor@dulcemayor.com.ar", RolUsuario.DISTRIBUIDOR, CLAVE_DEMO, false);
        usuarioService.crearUsuario("minorista@dulcemayor.com.ar", RolUsuario.MINORISTA, CLAVE_DEMO, false);
        usuarioService.crearUsuario("nuevo@dulcemayor.com.ar", RolUsuario.PREVENTOR);
    }

    /** E2-01 a E2-03: categorías, productos, stock inicial y vigencias de precio. */
    private void cargarCatalogo() {
        String[] nombresCategorias = { "Alfajores", "Caramelos", "Chicles", "Chocolates", "Gomitas" };
        List<CategoriaResponse> categorias = categoriaService.listarCategoriaActivo();
        for (String nombre : nombresCategorias) {
            boolean existe = categorias.stream().anyMatch(c -> c.nombre().equalsIgnoreCase(nombre));
            if (!existe) {
                categoriaService.crearCategoria(new CategoriaRequest(nombre));
            }
        }
        if (!productoService.listarActivos().isEmpty()) {
            return;
        }

        categorias = categoriaService.listarCategoriaActivo();
        byte[] imagen = leerRecurso("imagenes/imagen-prueba.png");
        Object[][] productosDemo = {
                { "ALF-001", "Alfajor de chocolate", "Alfajor relleno de dulce de leche con cobertura de chocolate.", "Caja x12", 12, "Alfajores", 12500.0, 30 },
                { "CAR-001", "Caramelos frutales", "Caramelos surtidos de frutas.", "Bolsa x100", 100, "Caramelos", 6800.0, 45 },
                { "CHI-001", "Chicles menta", "Chicles sabor menta en presentación individual.", "Caja x50", 50, "Chicles", 9400.0, 20 },
                { "CHO-001", "Chocolate con leche", "Tabletas de chocolate con leche.", "Caja x24", 24, "Chocolates", 18200.0, 18 },
                { "GOM-001", "Gomitas frutales", "Gomitas surtidas sabor frutal.", "Bolsa x1 kg", 1, "Gomitas", 7200.0, 25 }
        };
        for (Object[] dato : productosDemo) {
            CategoriaResponse categoria = categorias.stream()
                    .filter(c -> c.nombre().equalsIgnoreCase((String) dato[5]))
                    .findFirst().orElseThrow();
            ProductoRequest request = new ProductoRequest((String) dato[0], (String) dato[1], (String) dato[2],
                    (String) dato[3], (Integer) dato[4], categoria.id(),
                    new ArchivoSeeder("imagen-prueba.png", imagen));
            var producto = productoService.crear(request);
            vigenciaPrecioService.crearVigenciaPrecio(producto.id(),
                    new com.dulcemayor.dto.VigenciaPrecioRequest(LocalDate.now(), (Double) dato[6]));
            stockService.reponer(producto.id(), (Integer) dato[7], "Stock inicial de demostración");
        }
    }

    private byte[] leerRecurso(String ruta) {
        try {
            return new ClassPathResource(ruta).getContentAsByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de demo " + ruta, ex);
        }
    }

    private static final class ArchivoSeeder implements MultipartFile {
        private final String nombre;
        private final byte[] contenido;

        private ArchivoSeeder(String nombre, byte[] contenido) {
            this.nombre = nombre;
            this.contenido = contenido;
        }

        @Override public String getName() { return "imagen"; }
        @Override public String getOriginalFilename() { return nombre; }
        @Override public String getContentType() { return "image/png"; }
        @Override public boolean isEmpty() { return contenido.length == 0; }
        @Override public long getSize() { return contenido.length; }
        @Override public byte[] getBytes() { return contenido.clone(); }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(contenido); }
        @Override public void transferTo(File destino) throws IOException { Files.write(destino.toPath(), contenido); }
    }

    private String cargarImagen(String ruta, TipoImagen tipo) {
        try {
            ClassPathResource recurso = new ClassPathResource(ruta);
            return imagenService.crearImagen(recurso.getFilename(), recurso.getContentAsByteArray(), tipo).id();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de demo " + ruta, ex);
        }
    }

    /** E2-06: perfiles de demo del preventor y del minorista, y un kiosco en el centro de Mendoza. */
    private void cargarPuntosDeVenta() {
        Usuario usuarioPreventor = usuarioService.buscarUsuarioPorNombre("preventor@dulcemayor.com.ar");
        Preventor preventor = new Preventor();
        preventor.setNombre("Pedro");
        preventor.setApellido("Preventor");
        preventor.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        preventor.setTipoDocumento(TipoDocumento.DNI);
        preventor.setNumeroDocumento("30000001");
        preventor.setLegajo("P-001");
        preventor.setUsuario(usuarioPreventor);
        preventorRepository.save(preventor);

        Minorista minorista = new Minorista();
        minorista.setNombre("Marta");
        minorista.setApellido("Minorista");
        minorista.setFechaNacimiento(LocalDate.of(1985, 1, 1));
        minorista.setTipoDocumento(TipoDocumento.DNI);
        minorista.setNumeroDocumento("30000002");
        minorista.setUsuario(usuarioService.buscarUsuarioPorNombre("minorista@dulcemayor.com.ar"));
        minorista = minoristaRepository.save(minorista);

        var localidad = localidadRepository.findByEliminadoFalse(Sort.by("nombre")).get(0);
        puntoDeVentaService.crearPuntoDeVenta(new PuntoDeVentaRequest("Kiosco Centro", minorista.getId(),
                localidad.getId(), "San Martín", "850", "Centro", null, null, "Punto de venta de demostración",
                -32.8878, -68.8421), usuarioPreventor.getId());

        // E2-07: la dirección de entrega del minorista de demo se carga como desde su perfil
        minoristaService.modificarMinorista(minorista.getUsuario().getId(), new PerfilMinoristaRequest(
                minorista.getNombre(), minorista.getApellido(), minorista.getFechaNacimiento(), localidad.getId(),
                "San Martín", "850", "Centro", null, null, "Entregar en el kiosco", -32.8878, -68.8421));
    }

    private void cargarPedidos() { /* E6-04. */ }

    private boolean baseVacia() {
        for (EntityType<?> entidad : entityManager.getMetamodel().getEntities()) {
            Long cantidad = entityManager
                    .createQuery("select count(e) from " + entidad.getName() + " e", Long.class)
                    .getSingleResult();
            if (cantidad > 0) return false;
        }
        return true;
    }
}
