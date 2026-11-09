package com.dulcemayor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.repository.ImagenRepository;
import com.dulcemayor.service.CategoriaService;
import com.dulcemayor.service.ImagenService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

/**
 * Carga los datos iniciales solo cuando la base está vacía (igual que IntegradorN1).
 * Cada grupo de datos tiene su método y lo completa el issue indicado. Los datos se cargan
 * a través de los services, así pasan por sus validaciones.
 * Para volver a cargar todo, vaciar la base (borrar y recrear el esquema).
 */
@Component
public class DataSeeder implements CommandLineRunner, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EntityManager entityManager;
    private final CategoriaService categoriaService;
    private final ImagenRepository imagenRepository;
    private final ImagenService imagenService;
    private Environment environment;

    public DataSeeder(EntityManager entityManager, CategoriaService categoriaService,
            ImagenRepository imagenRepository, ImagenService imagenService) {
        this.entityManager = entityManager;
        this.categoriaService = categoriaService;
        this.imagenRepository = imagenRepository;
        this.imagenService = imagenService;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            cargarImagenPrueba();
        }
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

    private void cargarImagenPrueba() {
        if (imagenRepository.existsByNombre("imagen-prueba.png")) {
            return;
        }
        try {
            byte[] contenido = new ClassPathResource("imagenes/imagen-prueba.png").getContentAsByteArray();
            imagenService.crear(new ImagenCargaDTO(new ArchivoImagenPrueba(contenido), TipoImagen.PRODUCTO));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de prueba.", ex);
        }
    }

    /** E1-03: departamentos y localidades de Mendoza. */
    private void cargarUbicacion() {
        // Lo completa E1-03.
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
        // Lo completa E1-01.
    }

    /** E2-01 a E2-03: categorías, productos, stock inicial y vigencias de precio. */
    private void cargarCatalogo() {
        // E0-08: categorías de demo.
        for (String nombre : new String[] { "Alfajores", "Caramelos", "Chicles", "Chocolates", "Gomitas" }) {
            categoriaService.crearCategoria(new CategoriaRequest(nombre));
        }
        // E2-01, E2-02 y E2-03: productos, stock inicial y vigencias de precio.
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

    private record ArchivoImagenPrueba(byte[] bytes) implements MultipartFile {

        @Override
        public String getName() {
            return "archivo";
        }

        @Override
        public String getOriginalFilename() {
            return "imagen-prueba.png";
        }

        @Override
        public String getContentType() {
            return "image/png";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            Files.write(destination.toPath(), bytes);
        }
    }
}
