package com.zero.ecommerce;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;
import com.zero.ecommerce.services.VigenciaPrecioService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class AlertasPrecioIntegrationTest {

    private final MockMvc mvc;
    private final CategoriaRepository categoriaRepository;
    private final SubCategoriaRepository subCategoriaRepository;
    private final ProductoRepository productoRepository;
    private final VigenciaPrecioRepository vigenciaPrecioRepository;
    private final VigenciaPrecioService vigenciaPrecioService;

    AlertasPrecioIntegrationTest(@Autowired MockMvc mvc, @Autowired CategoriaRepository categoriaRepository,
            @Autowired SubCategoriaRepository subCategoriaRepository, @Autowired ProductoRepository productoRepository,
            @Autowired VigenciaPrecioRepository vigenciaPrecioRepository,
            @Autowired VigenciaPrecioService vigenciaPrecioService) {
        this.mvc = mvc;
        this.categoriaRepository = categoriaRepository;
        this.subCategoriaRepository = subCategoriaRepository;
        this.productoRepository = productoRepository;
        this.vigenciaPrecioRepository = vigenciaPrecioRepository;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @Test
    void muestraPrecioDeSetentaDiasAgrupadoConBadgeYEnlaceFiltrado() throws Exception {
        long alertasIniciales = vigenciaPrecioService.contarProductosConPrecioVencido();
        Producto vencido = crearProductoConPrecio("ALERTA-70", "Zapatilla alerta", "Calzado alerta", 70);
        crearProductoConPrecio("RECIENTE-30", "Remera reciente", "Remeras alerta", 30);
        String categoriaId = vencido.getSubCategoria().getCategoria().getId();

        mvc.perform(get("/admin/precios/alertas")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cantidadAlertasPrecio", alertasIniciales + 1))
                .andExpect(content().string(containsString("Zapatilla alerta")))
                .andExpect(content().string(containsString("70 días")))
                .andExpect(content().string(containsString("Calzado alerta")))
                .andExpect(content().string(containsString("badge badge-danger admin-nav-badge")))
                .andExpect(content().string(containsString(
                        "/admin/precios/actualizacion?alcance=CATEGORIA&amp;categoriaId=" + categoriaId)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Remera reciente"))));

        mvc.perform(get("/admin/precios/actualizacion")
                .param("alcance", "CATEGORIA")
                .param("categoriaId", categoriaId)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("alcance", "CATEGORIA"))
                .andExpect(model().attribute("categoriaId", categoriaId));
    }

    @Test
    void clienteNoPuedeAccederALasAlertas() throws Exception {
        mvc.perform(get("/admin/precios/alertas")
                .with(user("cliente@zero.com.ar").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private Producto crearProductoConPrecio(String codigo, String nombre, String subCategoriaNombre, int dias) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría " + codigo);
        categoria = categoriaRepository.save(categoria);

        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setNombre(subCategoriaNombre);
        subCategoria.setCategoria(categoria);
        subCategoria = subCategoriaRepository.save(subCategoria);

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setDescripcion("Producto de prueba para alertas");
        producto.setTalle("42");
        producto.setSubCategoria(subCategoria);
        producto = productoRepository.save(producto);

        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setFechaDesde(LocalDate.now().minusDays(dias));
        vigencia.setPrecio(25000);
        vigenciaPrecioRepository.save(vigencia);
        return producto;
    }
}
