package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.SubCategoriaService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ProductoIntegrationTest {

    private static final String BASE = "/admin/productos";
    private final MockMvc mvc;
    private final ProductoService service;
    private final ProductoRepository repository;
    private final CategoriaService categoriaService;
    private final SubCategoriaService subCategoriaService;
    private MockHttpSession sesion;

    ProductoIntegrationTest(@Autowired MockMvc mvc, @Autowired ProductoService service,
            @Autowired ProductoRepository repository, @Autowired CategoriaService categoriaService,
            @Autowired SubCategoriaService subCategoriaService) {
        this.mvc = mvc;
        this.service = service;
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.subCategoriaService = subCategoriaService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        // Se parte de un catálogo vacío: los productos del seeder (E2-02) se dan de baja y el
        // @Transactional del test lo revierte al terminar.
        repository.findAll().forEach(p -> p.setEliminado(true));
        repository.flush();
        // Productos no es configuración: también entra el ADMINISTRATIVO.
        sesion = login("admin@zero.com.ar", "Admin123!");
    }

    @Test
    void creaUnProductoConImagenYApareceEnElListado() throws Exception {
        SubCategoria calzado = subCategoria("Hombres", "Calzado");
        mvc.perform(multipartConCsrf(BASE, BASE + "/nuevo").file(imagen())
                .param("codigo", "ZAP-RUN-42").param("nombre", "Zapatilla Zero Run")
                .param("descripcion", "Zapatilla de running").param("talle", "42")
                .param("enOferta", "true").param("subCategoriaId", calzado.getId()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Producto creado correctamente."));
        Producto creado = service.buscarProductoPorCodigo("ZAP-RUN-42");
        assertThat(creado.getImagen()).isNotNull();
        assertThat(creado.isEnOferta()).isTrue();
        assertThat(creado.getSubCategoria().getId()).isEqualTo(calzado.getId());

        mvc.perform(get(BASE).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ZAP-RUN-42")))
                .andExpect(content().string(containsString("Hombres / Calzado")))
                .andExpect(content().string(containsString("Sin precio")))
                .andExpect(content().string(containsString("/imagen/" + creado.getImagen().getId())))
                .andExpect(content().string(containsString("data-target=\"#eliminar-" + creado.getId() + "\"")))
                .andExpect(content().string(containsString("action=\"" + BASE + "/" + creado.getId() + "/eliminar\"")));
        mvc.perform(get("/imagen/" + creado.getImagen().getId())).andExpect(status().isOk());
    }

    @Test
    void rechazaUnCodigoRepetidoYConservaLosDatos() throws Exception {
        SubCategoria ropa = subCategoria("Mujeres", "Ropa");
        service.crearProductoConImagen("REM-M", "Remera", "Algodón", "M", false, imagen(), ropa.getId());
        mvc.perform(multipartConCsrf(BASE, BASE + "/nuevo").file(imagen())
                .param("codigo", "rem-m").param("nombre", "Otra remera").param("descripcion", "Desc")
                .param("talle", "L").param("subCategoriaId", ropa.getId()))
                .andExpect(redirectedUrl(BASE + "/nuevo"))
                .andExpect(flash().attribute("error", "Ya existe un producto activo con el código rem-m."));
        assertThat(service.listarProductoActivo()).hasSize(1);

        // El formulario vuelve con lo que se había cargado.
        MvcResult error = mvc.perform(multipartConCsrf(BASE, BASE + "/nuevo")
                .param("codigo", "NUEVO").param("nombre", "Buzo Zero").param("descripcion", "Frisa")
                .param("talle", "XL").param("subCategoriaId", ropa.getId()))
                .andExpect(flash().attribute("error", "La imagen del producto es obligatoria.")).andReturn();
        mvc.perform(get(BASE + "/nuevo").session(sesion).flashAttrs(error.getFlashMap()))
                .andExpect(content().string(containsString("value=\"Buzo Zero\"")))
                .andExpect(content().string(containsString("La imagen del producto es obligatoria.")));
    }

    @Test
    void filtraPorCategoriaOfertaYTextoYPagina() throws Exception {
        SubCategoria calzado = subCategoria("Hombres", "Calzado");
        SubCategoria ropa = subCategoria("Mujeres", "Ropa");
        service.crearProductoConImagen("ZAP-42", "Zapatilla Montaña", "Trekking", "42", true, imagen(),
                calzado.getId());
        service.crearProductoConImagen("REM-S", "Remera Básica", "Algodón", "S", false, imagen(), ropa.getId());

        mvc.perform(get(BASE).session(sesion).param("categoria", calzado.getCategoria().getId()))
                .andExpect(content().string(containsString("ZAP-42")))
                .andExpect(content().string(not(containsString("REM-S"))));
        mvc.perform(get(BASE).session(sesion).param("oferta", "false"))
                .andExpect(content().string(containsString("REM-S")))
                .andExpect(content().string(not(containsString("ZAP-42"))));
        mvc.perform(get(BASE).session(sesion).param("buscar", "montana"))
                .andExpect(content().string(containsString("ZAP-42")))
                .andExpect(content().string(not(containsString("REM-S"))));
        mvc.perform(get(BASE).session(sesion).param("subcategoria", ropa.getId()))
                .andExpect(content().string(containsString("REM-S")))
                .andExpect(content().string(not(containsString("ZAP-42"))));

        for (int i = 1; i <= 10; i++) {
            service.crearProductoConImagen("MED-" + i, "Media " + i, "Media", "U", false, imagen(), ropa.getId());
        }
        // 12 productos: dos páginas, y los links de página conservan los filtros.
        mvc.perform(get(BASE).session(sesion).param("oferta", "false"))
                .andExpect(content().string(containsString("11 productos")))
                .andExpect(content().string(containsString("/admin/productos?oferta=false&amp;page=2")));
        // Orden por nombre: las 10 medias en la primera página, remera y zapatilla en la segunda.
        mvc.perform(get(BASE).session(sesion).param("page", "2"))
                .andExpect(content().string(containsString("ZAP-42")))
                .andExpect(content().string(containsString("REM-S")))
                .andExpect(content().string(not(containsString("MED-"))));
    }

    @Test
    void editaSinCambiarElCodigoNiLaImagenYDaDeBajaLogica() throws Exception {
        SubCategoria ropa = subCategoria("Mujeres", "Ropa");
        SubCategoria calzado = subCategoria("Hombres", "Calzado");
        Producto producto = service.crearProductoConImagen("REM-M", "Remera", "Algodón", "M", false, imagen(),
                ropa.getId());
        String imagenOriginal = producto.getImagen().getId();
        String editar = BASE + "/" + producto.getId() + "/editar";

        mvc.perform(get(editar).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"REM-M\" readonly")))
                .andExpect(content().string(containsString("value=\"" + ropa.getId() + "\" selected")))
                .andExpect(content().string(containsString("/imagen/" + imagenOriginal)));
        mvc.perform(multipartConCsrf(editar, editar).param("codigo", "OTRO").param("nombre", "Remera Pro")
                .param("descripcion", "Dry fit").param("talle", "L").param("enOferta", "true")
                .param("subCategoriaId", calzado.getId()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Producto modificado correctamente."));
        Producto modificado = service.buscarProducto(producto.getId());
        assertThat(modificado.getCodigo()).isEqualTo("REM-M");
        assertThat(modificado.getNombre()).isEqualTo("Remera Pro");
        assertThat(modificado.isEnOferta()).isTrue();
        assertThat(modificado.getSubCategoria().getId()).isEqualTo(calzado.getId());
        assertThat(modificado.getImagen().getId()).isEqualTo(imagenOriginal);

        mvc.perform(postConCsrf(BASE + "/" + producto.getId() + "/eliminar", BASE))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Producto eliminado correctamente."));
        assertThat(repository.findById(producto.getId()).orElseThrow().isEliminado()).isTrue();
        assertThat(service.listarProductoActivo()).isEmpty();
        mvc.perform(get(editar).session(sesion)).andExpect(status().isNotFound());
        // La subcategoría ya no tiene productos activos: se puede eliminar.
        subCategoriaService.eliminarSubCategoria(calzado.getId());
    }

    @Test
    void elSidebarMarcaProductosComoActivo() throws Exception {
        mvc.perform(get(BASE).session(sesion))
                .andExpect(content().string(containsString("href=\"/admin/productos\" class=\"admin-nav-item active\"")))
                .andExpect(content().string(containsString("No hay registros para mostrar.")));
    }

    private SubCategoria subCategoria(String categoria, String subCategoria) throws Exception {
        String categoriaId = categoriaService.buscarCategoriaPorNombre(categoria).getId();
        return subCategoriaService.listarSubCategoriaPorCategoria(categoriaId).stream()
                .filter(s -> s.getNombre().equals(subCategoria))
                .findFirst().orElseThrow();
    }

    private MockMultipartFile imagen() throws Exception {
        try (InputStream contenido = getClass().getResourceAsStream("/test-image.png")) {
            return new MockMultipartFile("imagen", "test-image.png", "image/png", contenido.readAllBytes());
        }
    }

    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession nueva = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(nueva).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return nueva;
    }

    // Obtiene el token del formulario renderizado: prueba también la integración Thymeleaf/Security.
    private CsrfToken tokenDe(String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        return (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
    }

    private MockMultipartHttpServletRequestBuilder multipartConCsrf(String destino, String formulario)
            throws Exception {
        CsrfToken token = tokenDe(formulario);
        return (MockMultipartHttpServletRequestBuilder) multipart(destino).session(sesion)
                .param(token.getParameterName(), token.getToken());
    }

    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        CsrfToken token = tokenDe(formulario);
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }
}
