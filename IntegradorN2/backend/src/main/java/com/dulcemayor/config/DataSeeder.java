package com.dulcemayor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.DepartamentoRequest;
import com.dulcemayor.dto.DepartamentoResponse;
import com.dulcemayor.dto.LocalidadRequest;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.service.CategoriaService;
import com.dulcemayor.service.DepartamentoService;
import com.dulcemayor.service.ImagenService;
import com.dulcemayor.service.LocalidadService;
import com.dulcemayor.service.UsuarioService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

/**
 * Carga los datos iniciales solo cuando la base está vacía (igual que IntegradorN1).
 * Cada grupo de datos tiene su método y lo completa el issue indicado. Los datos se cargan
 * a través de los services, así pasan por sus validaciones.
 * Para volver a cargar todo, vaciar la base (borrar y recrear el esquema).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EntityManager entityManager;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final ImagenService imagenService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;

    /** Clave de los usuarios de demo (no temporal, para no tener que cambiarla en cada base nueva). */
    public static final String CLAVE_DEMO = "Demo1234!";

    public DataSeeder(EntityManager entityManager, CategoriaService categoriaService,
            UsuarioService usuarioService, ImagenService imagenService,
            DepartamentoService departamentoService, LocalidadService localidadService) {
        this.entityManager = entityManager;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
        this.imagenService = imagenService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!baseVacia()) {
            log.info("La base ya tiene datos: no se ejecuta el seeder.");
            return;
        }
        log.info("Base vacía: cargando datos iniciales.");
        // El orden importa: cada grupo puede usar datos de los anteriores.
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

    /** E1-07: datos del mayorista Dulce Mayor y sus sucursales. */
    private void cargarEmpresa() {
        // Lo completa E1-07.
    }

    /** E1-07: formas de pago (efectivo, transferencia, billetera virtual). */
    private void cargarFormasDePago() {
        // Lo completa E1-07.
    }

    /** E1-01: usuarios de demo de cada rol (Jefe, Administrativo, Preventor, Distribuidor, Minorista). */
    private void cargarUsuarios() {
        usuarioService.crearUsuario("jefe@dulcemayor.com.ar", RolUsuario.JEFE, CLAVE_DEMO, false);
        usuarioService.crearUsuario("administrativo@dulcemayor.com.ar", RolUsuario.ADMINISTRATIVO, CLAVE_DEMO, false);
        usuarioService.crearUsuario("preventor@dulcemayor.com.ar", RolUsuario.PREVENTOR, CLAVE_DEMO, false);
        usuarioService.crearUsuario("distribuidor@dulcemayor.com.ar", RolUsuario.DISTRIBUIDOR, CLAVE_DEMO, false);
        usuarioService.crearUsuario("minorista@dulcemayor.com.ar", RolUsuario.MINORISTA, CLAVE_DEMO, false);
        // Alta como la haría el Jefe: clave "dulcemayor" y cambio obligatorio en el primer ingreso.
        usuarioService.crearUsuario("nuevo@dulcemayor.com.ar", RolUsuario.PREVENTOR);
        // Los datos personales (Persona/Empleado/Minorista) los agregan E2-04 y E2-05.
    }

    /** E2-01 a E2-03: categorías, productos, stock inicial y vigencias de precio. */
    private void cargarCatalogo() {
        // E0-08: categorías de demo.
        for (String nombre : new String[] { "Alfajores", "Caramelos", "Chicles", "Chocolates", "Gomitas" }) {
            categoriaService.crearCategoria(new CategoriaRequest(nombre));
        }
        // E1-05: imagen de prueba (se ve en /api/imagenes/{id}; E2-01 la reemplaza por las fotos de productos).
        cargarImagen("imagenes/imagen-prueba.png", TipoImagen.PRODUCTO);
        // E2-01, E2-02 y E2-03: productos, stock inicial y vigencias de precio.
    }

    /** Carga una imagen del classpath (src/main/resources) pasando por las validaciones de ImagenService. */
    private String cargarImagen(String ruta, TipoImagen tipo) {
        try {
            ClassPathResource recurso = new ClassPathResource(ruta);
            return imagenService.crearImagen(recurso.getFilename(), recurso.getContentAsByteArray(), tipo).id();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de demo " + ruta, ex);
        }
    }

    /** E2-06: puntos de venta de los minoristas de demo. */
    private void cargarPuntosDeVenta() {
        // Lo completa E2-06.
    }

    /** E6-04: pedidos, facturas y pagos de demo para reportes y dashboard. */
    private void cargarPedidos() {
        // Lo completa E6-04.
    }

    /**
     * La base está vacía si ninguna entidad JPA tiene filas. Se recorre el metamodelo,
     * así el chequeo incluye solas las entidades que se vayan agregando.
     */
    private boolean baseVacia() {
        for (EntityType<?> entidad : entityManager.getMetamodel().getEntities()) {
            Long cantidad = entityManager
                    .createQuery("select count(e) from " + entidad.getName() + " e", Long.class)
                    .getSingleResult();
            if (cantidad > 0) {
                return false;
            }
        }
        return true;
    }
}
