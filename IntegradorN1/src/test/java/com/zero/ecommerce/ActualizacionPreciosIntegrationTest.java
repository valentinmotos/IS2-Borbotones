package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class ActualizacionPreciosIntegrationTest {

    private final MockMvc mvc;
    private final CategoriaRepository categoriaRepository;
    private final SubCategoriaRepository subCategoriaRepository;
    private final ProductoRepository productoRepository;
    private final VigenciaPrecioRepository vigenciaPrecioRepository;

    ActualizacionPreciosIntegrationTest(@Autowired MockMvc mvc, @Autowired CategoriaRepository categoriaRepository,
            @Autowired SubCategoriaRepository subCategoriaRepository, @Autowired ProductoRepository productoRepository,
            @Autowired VigenciaPrecioRepository vigenciaPrecioRepository) {
        this.mvc = mvc;
        this.categoriaRepository = categoriaRepository;
        this.subCategoriaRepository = subCategoriaRepository;
        this.productoRepository = productoRepository;
        this.vigenciaPrecioRepository = vigenciaPrecioRepository;
    }

    @Test
    void adminPuedePrevisualizarYConfirmarUnAumentoPorCategoria() throws Exception {
        Producto producto = productoConPrecio();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/admin/precios/actualizacion")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Actualización masiva de precios")));

        mvc.perform(post("/admin/precios/actualizacion/vista-previa")
                .session(session)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                .param("alcance", "CATEGORIA")
                .param("categoriaId", producto.getSubCategoria().getCategoria().getId())
                .param("porcentaje", "15")
                .param("fechaDesde", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vista previa")))
                .andExpect(content().string(containsString("1.150")));

        mvc.perform(post("/admin/precios/actualizacion/confirmar")
                .session(session)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                )
                .andExpect(redirectedUrl("/admin/precios/actualizacion"))
                .andExpect(flash().attribute("exito", "Precios actualizados para 1 producto."));

        var vigenciaActual = vigenciaPrecioRepository
                .findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(producto.getId()).orElseThrow();
        assertThat(vigenciaActual.getPrecio()).isEqualTo(1150);
        assertThat(vigenciaActual.getFechaDesde()).isEqualTo(LocalDate.now());
        assertThat(vigenciaPrecioRepository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(producto.getId()))
                .hasSize(2);
    }

    @Test
    void noPermiteConfirmarSinUnaVistaPrevia() throws Exception {
        Producto producto = productoConPrecio();

        mvc.perform(post("/admin/precios/actualizacion/confirmar")
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf()))
                .andExpect(redirectedUrl("/admin/precios/actualizacion"))
                .andExpect(flash().attribute("error", "Primero generá una vista previa de la actualización."));

        assertThat(vigenciaPrecioRepository
                .findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(producto.getId())).hasSize(1);
    }

    @Test
    void exigeOtraVistaPreviaSiElPrecioCambioAntesDeConfirmar() throws Exception {
        Producto producto = productoConPrecio();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/admin/precios/actualizacion/vista-previa")
                .session(session)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf())
                .param("alcance", "CATEGORIA")
                .param("categoriaId", producto.getSubCategoria().getCategoria().getId())
                .param("porcentaje", "15")
                .param("fechaDesde", LocalDate.now().toString()))
                .andExpect(status().isOk());

        VigenciaPrecio vigente = vigenciaPrecioRepository
                .findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(producto.getId()).orElseThrow();
        vigente.setPrecio(1200);
        vigenciaPrecioRepository.saveAndFlush(vigente);

        mvc.perform(post("/admin/precios/actualizacion/confirmar")
                .session(session)
                .with(user("admin@zero.com.ar").roles("ADMINISTRATIVO"))
                .with(csrf()))
                .andExpect(redirectedUrl("/admin/precios/actualizacion"))
                .andExpect(flash().attribute("error",
                        "Los precios cambiaron desde la vista previa. Revisá los valores antes de confirmar."));

        assertThat(vigenciaPrecioRepository
                .findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(producto.getId())).hasSize(1);
        assertThat(vigenciaPrecioRepository
                .findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(producto.getId()).orElseThrow().getPrecio())
                .isEqualTo(1200);
    }

    @Test
    void clienteNoPuedeAccederALaActualizacionMasiva() throws Exception {
        mvc.perform(get("/admin/precios/actualizacion")
                .with(user("cliente@zero.com.ar").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private Producto productoConPrecio() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Hombres prueba inflación");
        categoria = categoriaRepository.save(categoria);
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setNombre("Calzado prueba inflación");
        subCategoria.setCategoria(categoria);
        subCategoria = subCategoriaRepository.save(subCategoria);
        Producto producto = new Producto();
        producto.setCodigo("ZAP-INFLACION-42");
        producto.setNombre("Zapatilla inflación");
        producto.setDescripcion("Producto de prueba");
        producto.setTalle("42");
        producto.setSubCategoria(subCategoria);
        producto = productoRepository.save(producto);
        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setFechaDesde(LocalDate.now().minusDays(30));
        vigencia.setPrecio(1000);
        vigenciaPrecioRepository.save(vigencia);
        return producto;
    }
}
