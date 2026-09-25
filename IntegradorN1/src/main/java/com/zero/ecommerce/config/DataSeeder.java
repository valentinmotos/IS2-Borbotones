package com.zero.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.TipoEmpleado;

/**
 * Carga los datos iniciales cuando la base está vacía. Cada grupo de datos tiene su método,
 * que completa el issue indicado. Para volver a cargar todo, borrar data/zero.db.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(EntityManager entityManager, PasswordEncoder passwordEncoder) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
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

    private void cargarUbicacion() {
        // E1-03: Argentina, provincias, departamentos de Mendoza y localidades de Gran Mendoza.
        for (String nombre : new String[] { "Argentina", "Bolivia", "Brasil", "Chile",
                "Colombia", "España", "Italia", "Paraguay", "Perú", "Uruguay", "Venezuela" }) {
            Nacionalidad nacionalidad = new Nacionalidad();
            nacionalidad.setNombre(nombre);
            entityManager.persist(nacionalidad);
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
        // E1-08: Efectivo, Transferencia y Mercado Pago.
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
