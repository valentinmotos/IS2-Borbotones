package com.zero.ecommerce.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.TipoEmpleado;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

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

    public DataSeeder(EntityManager entityManager, PasswordEncoder passwordEncoder, PaisService paisService,
            ProvinciaService provinciaService, DepartamentoService departamentoService,
            LocalidadService localidadService) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
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

    private void cargarEmpresa() {
        // E1-07: empresa Zero como SEDE_CENTRAL.
    }

    private void cargarCatalogo() {
        // E2-02: productos de demostración con imágenes.
        // E2-03: precios iniciales de los productos.
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
