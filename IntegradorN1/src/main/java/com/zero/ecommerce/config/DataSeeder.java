package com.zero.ecommerce.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.entities.enums.TipoEmpleado;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.entities.enums.TipoEmpresa;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.ImagenService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.ProvinciaService;
import com.zero.ecommerce.services.SubCategoriaService;
import com.zero.ecommerce.services.VigenciaPrecioService;
import com.zero.ecommerce.utils.ArchivoEnMemoria;

/**
 * Carga los datos iniciales cuando la base está vacía. Cada grupo de datos tiene su método,
 * que completa el issue indicado. Para volver a cargar todo, borrar data/zero.db.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // E1-03: Argentina con sus 24 jurisdicciones, los 18 departamentos de Mendoza y las
    // principales localidades de Gran Mendoza. Pasa por los services para aplicar sus validaciones.
    private static final List<String> PROVINCIAS_ARGENTINA = List.of("Buenos Aires",
            "Ciudad Autónoma de Buenos Aires", "Catamarca", "Chaco", "Chubut", "Córdoba", "Corrientes",
            "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja", "Mendoza", "Misiones", "Neuquén",
            "Río Negro", "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe", "Santiago del Estero",
            "Tierra del Fuego, Antártida e Islas del Atlántico Sur", "Tucumán");

    private static final List<String> DEPARTAMENTOS_MENDOZA = List.of("Capital", "General Alvear",
            "Godoy Cruz", "Guaymallén", "Junín", "La Paz", "Las Heras", "Lavalle", "Luján de Cuyo", "Maipú",
            "Malargüe", "Rivadavia", "San Carlos", "San Martín", "San Rafael", "Santa Rosa", "Tunuyán",
            "Tupungato");

    // Departamento → (localidad → código postal).
    private static final Map<String, Map<String, String>> LOCALIDADES_GRAN_MENDOZA = localidadesGranMendoza();

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final PaisService paisService;
    private final ProvinciaService provinciaService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;
    private final EmpresaService empresaService;
    private final CategoriaService categoriaService;
    private final SubCategoriaService subCategoriaService;
    private final ImagenService imagenService;
    private final ProductoService productoService;
    private final VigenciaPrecioService vigenciaPrecioService;

    private static final double PRECIO_INICIAL_DEMO = 10000;

    public DataSeeder(EntityManager entityManager, PasswordEncoder passwordEncoder, PaisService paisService,
            ProvinciaService provinciaService, DepartamentoService departamentoService,
            LocalidadService localidadService, EmpresaService empresaService, CategoriaService categoriaService,
            SubCategoriaService subCategoriaService, ImagenService imagenService, ProductoService productoService,
            VigenciaPrecioService vigenciaPrecioService) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
        this.empresaService = empresaService;
        this.categoriaService = categoriaService;
        this.subCategoriaService = subCategoriaService;
        this.imagenService = imagenService;
        this.productoService = productoService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!baseVacia()) {
            cargarUsuariosFaltantes();
            log.info("La base ya tiene datos: no se ejecuta el seeder.");
            return;
        }
        log.info("Base vacía: cargando datos iniciales.");
        // El orden importa: cada grupo puede usar datos de los anteriores.
        cargarNacionalidades();
        cargarUbicacion();
        cargarUsuarios();
        cargarCategorias();
        cargarFormasDePago();
        cargarEmpresa();
        cargarCatalogo();
        cargarProveedores();
        cargarVentas();
    }

    private void cargarUsuariosFaltantes() {
        cargarEmpleadoSiNoExiste("Jefa", "Zero", "jefe@zero.com.ar", "Jefe123!",
                TipoEmpleado.JEFE, RolUsuario.JEFE);
        cargarEmpleadoSiNoExiste("Administrativo", "Zero", "admin@zero.com.ar", "Admin123!",
                TipoEmpleado.ADMINISTRATIVO, RolUsuario.ADMINISTRATIVO);

        cargarClienteSiNoExiste("cliente@zero.com.ar", "Cliente123", RolUsuario.CLIENTE);
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

    private void cargarNacionalidades() {
        for (String nombre : new String[] { "Argentina", "Bolivia", "Brasil", "Chile",
                "Colombia", "España", "Italia", "Paraguay", "Perú", "Uruguay", "Venezuela" }) {
            Nacionalidad nacionalidad = new Nacionalidad();
            nacionalidad.setNombre(nombre);
            entityManager.persist(nacionalidad);
        }
    }

    private static Map<String, Map<String, String>> localidadesGranMendoza() {
        Map<String, Map<String, String>> localidades = new LinkedHashMap<>();
        localidades.put("Capital", Map.of("Ciudad de Mendoza", "5500"));
        localidades.put("Godoy Cruz", Map.of("Godoy Cruz", "5501", "Gobernador Benegas", "5501",
                "Villa Hipódromo", "5501"));
        localidades.put("Guaymallén", Map.of("Villa Nueva", "5521", "Dorrego", "5519", "San José", "5519",
                "Buena Nueva", "5523", "Rodeo de la Cruz", "5525"));
        localidades.put("Las Heras", Map.of("Las Heras", "5539", "El Challao", "5539", "El Plumerillo", "5541"));
        localidades.put("Luján de Cuyo", Map.of("Luján de Cuyo", "5507", "Mayor Drummond", "5507",
                "Chacras de Coria", "5505", "Carrodilla", "5505", "Vistalba", "5509"));
        localidades.put("Maipú", Map.of("Maipú", "5515", "Coquimbito", "5513", "Luzuriaga", "5513",
                "Gutiérrez", "5511", "Russell", "5517", "Rodeo del Medio", "5529"));
        return localidades;
    }

    private void cargarUbicacion() {
        try {
            Pais argentina = paisService.crearPais("Argentina");
            for (String nombreProvincia : PROVINCIAS_ARGENTINA) {
                Provincia provincia = provinciaService.crearProvincia(nombreProvincia, argentina.getId());
                if (nombreProvincia.equals("Mendoza")) {
                    cargarDepartamentosMendoza(provincia);
                }
            }
        } catch (ErrorServiceException e) {
            throw new IllegalStateException("Los datos iniciales de ubicación no son válidos: " + e.getMessage(), e);
        }
    }

    private void cargarDepartamentosMendoza(Provincia mendoza) throws ErrorServiceException {
        for (String nombreDepartamento : DEPARTAMENTOS_MENDOZA) {
            Departamento departamento = departamentoService.crearDepartamento(nombreDepartamento, mendoza.getId());
            Map<String, String> localidades = LOCALIDADES_GRAN_MENDOZA.getOrDefault(nombreDepartamento, Map.of());
            for (Map.Entry<String, String> localidad : localidades.entrySet()) {
                localidadService.crearLocalidad(localidad.getKey(), localidad.getValue(), departamento.getId());
            }
        }
    }

    private void cargarUsuarios() {
        cargarEmpleado("Jefa", "Zero", "jefe@zero.com.ar", "Jefe123!", TipoEmpleado.JEFE, RolUsuario.JEFE);
        cargarEmpleado("Administrativo", "Zero", "admin@zero.com.ar", "Admin123!",
                TipoEmpleado.ADMINISTRATIVO, RolUsuario.ADMINISTRATIVO);
        // E2-07: cliente de prueba con usuario activo.

        cargarCliente("cliente@zero.com.ar", "Cliente123!", RolUsuario.CLIENTE);
    }

    private void cargarCliente(String correo, String clave, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(correo);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(rol);
        entityManager.persist(usuario);
    }

    private void cargarClienteSiNoExiste(String correo, String clave, RolUsuario rol) {
        boolean existe = entityManager.createQuery(
                        "select count(u) from Usuario u where lower(u.nombreUsuario) = lower(:nombreUsuario)", Long.class)
                .setParameter("nombreUsuario", correo)
                .getSingleResult() > 0;
        if (!existe) {
            cargarCliente(correo, clave, rol);
            log.info("Usuario cliente demo creado: {}", correo);
        }
    }

    private void cargarEmpleado(String nombre, String apellido, String correo, String clave,
            TipoEmpleado tipoEmpleado, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(correo);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(rol);
        entityManager.persist(usuario);

        Empleado empleado = new Empleado();
        empleado.setNombre(nombre);
        empleado.setApellido(apellido);
        empleado.setTipoEmpleado(tipoEmpleado);
        empleado.setUsuario(usuario);
        entityManager.persist(empleado);
    }

    private void cargarEmpleadoSiNoExiste(String nombre, String apellido, String correo, String clave,
            TipoEmpleado tipoEmpleado, RolUsuario rol) {
        boolean existe = entityManager.createQuery(
                "select count(u) from Usuario u where lower(u.nombreUsuario) = lower(:nombreUsuario)", Long.class)
                .setParameter("nombreUsuario", correo)
                .getSingleResult() > 0;
        if (!existe) {
            cargarEmpleado(nombre, apellido, correo, clave, tipoEmpleado, rol);
            log.info("Usuario demo creado: {}", correo);
        }
    }

    private void cargarCategorias() {
        String[][] categorias = {
                { "Hombres", "Ropa", "Calzado", "Accesorios" },
                { "Mujeres", "Ropa", "Calzado", "Accesorios" },
                { "Niños", "Ropa", "Calzado", "Accesorios" },
                { "Accesorios", "Bolsos", "Joyas", "Marroquinería" }
        };

        for (String[] datos : categorias) {
            Categoria categoria = new Categoria();
            categoria.setNombre(datos[0]);
            entityManager.persist(categoria);

            for (int i = 1; i < datos.length; i++) {
                SubCategoria subCategoria = new SubCategoria();
                subCategoria.setCategoria(categoria);
                subCategoria.setNombre(datos[i]);
                entityManager.persist(subCategoria);
            }
        }
    }

    private void cargarFormasDePago() {
        cargarFormaDePago(TipoPago.EFECTIVO, "Efectivo");
        cargarFormaDePago(TipoPago.TRANSFERENCIA, "Transferencia");
        cargarFormaDePago(TipoPago.BILLETERA_VIRTUAL, "Mercado Pago");
    }

    private void cargarFormaDePago(TipoPago tipoPago, String observacion) {
        FormaDePago formaDePago = new FormaDePago();
        formaDePago.setTipoPago(tipoPago);
        formaDePago.setObservacion(observacion);
        entityManager.persist(formaDePago);
    }

    // E1-07: la empresa Zero como SEDE_CENTRAL. La configuración de correo no se carga porque
    // lleva credenciales reales: se completa desde Configuración → Correo.
    private void cargarEmpresa() {
        try {
            DireccionForm direccion = new DireccionForm();
            direccion.setCalle("Av. San Martín");
            direccion.setNumeracion("1250");
            direccion.setReferencia("Local a la calle");
            direccion.setLocalidadId(localidadService.buscarLocalidadPorNombre("Ciudad de Mendoza").getId());
            empresaService.crearEmpresa("Zero Indumentaria Deportiva S.A.", "30-71567890-6", TipoEmpresa.SEDE_CENTRAL,
                    direccion, "contacto@zero.com.ar", "+54 261 423-1250", TipoTelefono.FIJO);
        } catch (ErrorServiceException e) {
            throw new IllegalStateException("Los datos iniciales de la empresa no son válidos: " + e.getMessage(), e);
        }
    }

    // E2-02: 20 productos de demostración en las 12 subcategorías, algunos en oferta y algunos en varios
    // talles (cada talle es un producto). Las fotos están en resources/seed/img (ver CREDITOS.md) y se
    // cargan con ImagenService; cada producto tiene su propia Imagen aunque compartan la foto.
    // Columnas: código, nombre, talle, categoría, subcategoría, en oferta, foto, descripción.
    private static final String[][] CATALOGO = {
            { "REM-DRY-H-M", "Remera Zero Dry Fit Hombre", "M", "Hombres", "Ropa", "no", "remera-dry-fit-hombre.jpg",
                    "Remera de entrenamiento de secado rápido, liviana y respirable." },
            { "REM-DRY-H-L", "Remera Zero Dry Fit Hombre", "L", "Hombres", "Ropa", "no", "remera-dry-fit-hombre.jpg",
                    "Remera de entrenamiento de secado rápido, liviana y respirable." },
            { "SHO-RUN-H-M", "Short Zero Run Hombre", "M", "Hombres", "Ropa", "si", "short-run-hombre.jpg",
                    "Short de running con calza interna y bolsillo trasero con cierre." },
            { "ZAP-RUN-41", "Zapatilla Zero Run", "41", "Hombres", "Calzado", "no", "zapatilla-run-hombre.jpg",
                    "Zapatilla de running con amortiguación en la entresuela y capellada de malla." },
            { "ZAP-RUN-42", "Zapatilla Zero Run", "42", "Hombres", "Calzado", "no", "zapatilla-run-hombre.jpg",
                    "Zapatilla de running con amortiguación en la entresuela y capellada de malla." },
            { "GOR-TRN-U", "Gorra Zero Training", "Único", "Hombres", "Accesorios", "si", "gorra-training.jpg",
                    "Gorra de algodón con visera curva y cierre regulable." },
            { "CAL-FIT-S", "Calza Zero Fit", "S", "Mujeres", "Ropa", "no", "calza-fit-mujer.jpg",
                    "Calza larga de tiro alto con tela compresiva que no se transparenta." },
            { "CAL-FIT-M", "Calza Zero Fit", "M", "Mujeres", "Ropa", "no", "calza-fit-mujer.jpg",
                    "Calza larga de tiro alto con tela compresiva que no se transparenta." },
            { "TOP-MOV-M", "Top Deportivo Zero Move", "M", "Mujeres", "Ropa", "si", "top-deportivo-mujer.jpg",
                    "Top de sujeción media con espalda deportiva y tazas removibles." },
            { "ZAP-FLW-38", "Zapatilla Zero Flow Mujer", "38", "Mujeres", "Calzado", "no", "zapatilla-flow-mujer.jpg",
                    "Zapatilla liviana para entrenamiento y uso diario, con suela de goma antideslizante." },
            { "ZAP-FLW-39", "Zapatilla Zero Flow Mujer", "39", "Mujeres", "Calzado", "no", "zapatilla-flow-mujer.jpg",
                    "Zapatilla liviana para entrenamiento y uso diario, con suela de goma antideslizante." },
            { "COL-YOG-U", "Colchoneta de Yoga Zero", "Único", "Mujeres", "Accesorios", "no", "colchoneta-yoga.jpg",
                    "Colchoneta antideslizante de 6 mm con correa para transportarla." },
            { "CON-KID-8", "Conjunto Deportivo Zero Kids", "8", "Niños", "Ropa", "si", "conjunto-deportivo-kids.jpg",
                    "Campera y pantalón de frisa liviana, ideales para la escuela y el deporte." },
            { "CON-KID-10", "Conjunto Deportivo Zero Kids", "10", "Niños", "Ropa", "si", "conjunto-deportivo-kids.jpg",
                    "Campera y pantalón de frisa liviana, ideales para la escuela y el deporte." },
            { "ZAP-KID-24", "Zapatilla Zero Kids", "24", "Niños", "Calzado", "no", "zapatilla-kids.jpg",
                    "Zapatilla infantil con cierre de abrojo, fácil de poner y sacar." },
            { "SAC-KID-U", "Mochila Saco Zero Kids", "Único", "Niños", "Accesorios", "no", "mochila-saco-kids.jpg",
                    "Mochila tipo saco con cordones, para llevar la ropa de gimnasia." },
            { "MOC-URB-U", "Mochila Zero Urban", "Único", "Accesorios", "Bolsos", "no", "mochila-urban.jpg",
                    "Mochila de 20 litros con compartimento acolchado para notebook." },
            { "BOL-GYM-U", "Bolso Zero Gym", "Único", "Accesorios", "Bolsos", "no", "bolso-gym.jpg",
                    "Bolso de entrenamiento de tela resistente con manijas reforzadas." },
            { "REL-SPT-U", "Reloj Zero Sport", "Único", "Accesorios", "Joyas", "si", "reloj-sport.jpg",
                    "Reloj deportivo resistente al agua, con malla de silicona." },
            { "BIL-CUE-U", "Billetera Zero de Cuero", "Único", "Accesorios", "Marroquinería", "no", "billetera-cuero.jpg",
                    "Billetera de cuero negro con tarjetero y monedero." }
    };

    private void cargarCatalogo() {
        try {
            for (String[] p : CATALOGO) {
                String idImagen = imagenService.crearImagen(leerImagenSeed(p[6]), TipoImagen.PRODUCTO).getId();
                productoService.crearProducto(p[0], p[1], p[7], p[2], p[5].equals("si"), idImagen,
                        buscarSubCategoria(p[3], p[4]));
            }
        } catch (ErrorServiceException e) {
            throw new IllegalStateException("Los datos iniciales del catálogo no son válidos: " + e.getMessage(), e);
        }
        try {
            for (Producto producto : productoService.listarProductoActivo()) {
                var precioInicial = vigenciaPrecioService.crearVigenciaPrecio(producto.getId(), LocalDate.now(),
                        PRECIO_INICIAL_DEMO);
                // El catálogo simula precios con antigüedad para poder probar de inmediato una actualización.
                precioInicial.setFechaDesde(LocalDate.now().minusMonths(3));
            }
        } catch (ErrorServiceException e) {
            throw new IllegalStateException("No se pudieron cargar los precios iniciales: " + e.getMessage(), e);
        }
    }

    private ArchivoEnMemoria leerImagenSeed(String archivo) {
        try (InputStream contenido = new ClassPathResource("seed/img/" + archivo).getInputStream()) {
            return new ArchivoEnMemoria(archivo, "image/jpeg", contenido.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró la imagen de demostración seed/img/" + archivo, e);
        }
    }

    private String buscarSubCategoria(String categoria, String subCategoria) throws ErrorServiceException {
        String idCategoria = categoriaService.buscarCategoriaPorNombre(categoria).getId();
        return subCategoriaService.listarSubCategoriaPorCategoria(idCategoria).stream()
                .filter(s -> s.getNombre().equals(subCategoria))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("No existe la subcategoría " + categoria + " / " + subCategoria))
                .getId();
    }

    private void cargarProveedores() {
        // E3-01: 4 proveedores con sus contactos.
        // E3-04: compras de demostración recibidas (stock inicial).
    }

    private void cargarVentas() {
        // E4-04: órdenes en todos los estados.
        // E5-01 / E6-03: ventas pagadas en varios meses para reportes y dashboard.
    }
}
