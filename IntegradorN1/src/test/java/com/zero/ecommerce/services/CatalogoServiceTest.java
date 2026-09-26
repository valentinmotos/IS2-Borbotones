package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private ProductoService productoService;
    @Mock
    private StockService stockService;
    @Mock
    private VigenciaPrecioService vigenciaPrecioService;
    @InjectMocks
    private CatalogoService service;

    @Test
    void listaSoloActivosConStockYPrecioVigente() throws Exception {
        Producto visible = producto("p1", "Disponible", "cat-1", "sub-1", true);
        Producto sinStock = producto("p2", "Agotado", "cat-1", "sub-1", false);
        Producto sinPrecio = producto("p3", "Sin precio", "cat-1", "sub-1", false);
        when(productoService.listarProductoActivo()).thenReturn(List.of(visible, sinStock, sinPrecio));
        when(stockService.buscarStockActual("p1")).thenReturn(7);
        when(stockService.buscarStockActual("p2")).thenReturn(0);
        when(stockService.buscarStockActual("p3")).thenReturn(2);
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(12500d);
        when(vigenciaPrecioService.buscarPrecioVigente("p3"))
                .thenThrow(new ErrorServiceException("Sin precio"));

        assertThat(service.listar(CatalogoFiltro.todos()))
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.id()).isEqualTo("p1");
                    assertThat(dto.precio()).isEqualTo(12500d);
                    assertThat(dto.stock()).isEqualTo(7);
                    assertThat(dto.oferta()).isTrue();
                    assertThat(dto.imagen()).isEqualTo("img-p1");
                });
    }

    @Test
    void filtraPorCategoriaSubcategoriaYOferta() throws Exception {
        Producto calzado = producto("p1", "Zapatilla", "cat-1", "sub-1", true);
        Producto ropa = producto("p2", "Remera", "cat-1", "sub-2", false);
        Producto otraCategoria = producto("p3", "Bolso", "cat-2", "sub-3", true);
        when(productoService.listarProductoActivo()).thenReturn(List.of(calzado, ropa, otraCategoria));
        when(stockService.buscarStockActual("p1")).thenReturn(3);
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(20000d);

        assertThat(service.listarPorSubCategoria("cat-1", "sub-1"))
                .extracting(dto -> dto.id()).containsExactly("p1");
        assertThat(service.listar(CatalogoFiltro.ofertas()))
                .extracting(dto -> dto.id()).containsExactly("p1");
    }

    @Test
    void unFiltroNuloEquivaleATodoElCatalogo() throws Exception {
        Producto producto = producto("p1", "Producto", "cat-1", "sub-1", false);
        when(productoService.listarProductoActivo()).thenReturn(List.of(producto));
        when(stockService.buscarStockActual("p1")).thenReturn(1);
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1000d);

        assertThat(service.listar(null)).hasSize(1);
    }

    private Producto producto(String id, String nombre, String categoriaId, String subCategoriaId, boolean oferta) {
        Categoria categoria = new Categoria();
        categoria.setId(categoriaId);
        categoria.setNombre("Categoria " + categoriaId);
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setId(subCategoriaId);
        subCategoria.setNombre("Subcategoria " + subCategoriaId);
        subCategoria.setCategoria(categoria);
        Imagen imagen = new Imagen();
        imagen.setId("img-" + id);
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo("COD-" + id);
        producto.setNombre(nombre);
        producto.setTalle("M");
        producto.setEnOferta(oferta);
        producto.setSubCategoria(subCategoria);
        producto.setImagen(imagen);
        return producto;
    }
}
