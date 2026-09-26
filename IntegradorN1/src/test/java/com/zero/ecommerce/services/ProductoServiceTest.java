package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;

import com.zero.ecommerce.dto.FilaTablaImagenDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;
    @Mock
    private SubCategoriaService subCategoriaService;
    @Mock
    private ImagenService imagenService;
    @Mock
    private VigenciaPrecioService vigenciaPrecioService;
    @Mock
    private StockService stockService;
    @InjectMocks
    private ProductoService service;

    private final SubCategoria calzadoHombres = subCategoria("sub-1", "Calzado", categoria("cat-1", "Hombres"));
    private final SubCategoria ropaMujeres = subCategoria("sub-2", "Ropa", categoria("cat-2", "Mujeres"));

    @Test
    void rechazaCodigoDuplicadoSinImportarMayusculas() {
        when(repository.findByEliminadoFalseOrderByNombreAscTalleAsc())
                .thenReturn(List.of(producto("1", "ZAP-42", "Zapatilla Run", "42", false, calzadoHombres)));
        assertThatThrownBy(() -> service.crearProducto(" zap-42 ", "Zapatilla", "Desc", "42", false, "img",
                "sub-1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe un producto activo con el código zap-42.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null, Nombre, Desc, M, sub-1, El código del producto es obligatorio.",
            "COD-1, ' ', Desc, M, sub-1, El nombre del producto es obligatorio.",
            "COD-1, Nombre, null, M, sub-1, La descripción del producto es obligatoria.",
            "COD-1, Nombre, Desc, '', sub-1, El talle del producto es obligatorio.",
            "COD-1, Nombre, Desc, M, null, La subcategoría del producto es obligatoria." })
    void exigeLosCamposObligatorios(String codigo, String nombre, String descripcion, String talle,
            String idSubCategoria, String mensaje) {
        assertThatThrownBy(() -> service.crearProducto(codigo, nombre, descripcion, talle, false, "img",
                idSubCategoria))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage(mensaje);
        verify(repository, never()).save(any());
    }

    @Test
    void exigeImagenAlCrearDesdeElFormularioSinGuardarNada() {
        MockMultipartFile vacio = new MockMultipartFile("imagen", "vacia.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.crearProductoConImagen("COD-1", "Remera", "Desc", "M", false, vacio,
                "sub-2"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La imagen del producto es obligatoria.");
        assertThatThrownBy(() -> service.crearProductoConImagen("COD-1", "Remera", "Desc", "M", false, null,
                "sub-2"))
                .hasMessage("La imagen del producto es obligatoria.");
        verifyNoInteractions(imagenService);
        verify(repository, never()).save(any());
    }

    @Test
    void noGuardaLaImagenSiLosDatosSonInvalidos() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[] { 1 });
        assertThatThrownBy(() -> service.crearProductoConImagen("COD-1", "Remera", "", "M", false, archivo,
                "sub-2"))
                .hasMessage("La descripción del producto es obligatoria.");
        verifyNoInteractions(imagenService);
    }

    @Test
    void creaElProductoSinEspaciosEnLosExtremos() throws Exception {
        Imagen imagen = new Imagen();
        imagen.setId("img");
        when(subCategoriaService.buscarSubCategoria("sub-2")).thenReturn(ropaMujeres);
        when(imagenService.buscarImagen("img")).thenReturn(imagen);
        service.crearProducto(" REM-M ", " Remera Zero ", " Algodón ", " M ", true, "img", "sub-2");
        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(guardado.capture());
        Producto producto = guardado.getValue();
        assertThat(producto.getCodigo()).isEqualTo("REM-M");
        assertThat(producto.getNombre()).isEqualTo("Remera Zero");
        assertThat(producto.getDescripcion()).isEqualTo("Algodón");
        assertThat(producto.getTalle()).isEqualTo("M");
        assertThat(producto.isEnOferta()).isTrue();
        assertThat(producto.getSubCategoria()).isSameAs(ropaMujeres);
        assertThat(producto.getImagen()).isSameAs(imagen);
    }

    @Test
    void rechazaUnaSubcategoriaDeUnaCategoriaEliminada() throws Exception {
        ropaMujeres.getCategoria().setEliminado(true);
        when(subCategoriaService.buscarSubCategoria("sub-2")).thenReturn(ropaMujeres);
        assertThatThrownBy(() -> service.crearProducto("REM-M", "Remera", "Desc", "M", false, "img", "sub-2"))
                .hasMessage("La categoría de la subcategoría seleccionada fue eliminada.");
    }

    @Test
    void modificarSinImagenNuevaConservaLaActualYElCodigo() throws Exception {
        Producto actual = producto("1", "REM-M", "Remera", "M", false, ropaMujeres);
        Imagen imagen = new Imagen();
        imagen.setId("img");
        actual.setImagen(imagen);
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        when(subCategoriaService.buscarSubCategoria("sub-1")).thenReturn(calzadoHombres);
        service.modificarProductoConImagen("1", "Remera Pro", "Nueva", "L", true, null, "sub-1");
        assertThat(actual.getImagen()).isSameAs(imagen);
        assertThat(actual.getCodigo()).isEqualTo("REM-M");
        assertThat(actual.getNombre()).isEqualTo("Remera Pro");
        assertThat(actual.getTalle()).isEqualTo("L");
        assertThat(actual.isEnOferta()).isTrue();
        assertThat(actual.getSubCategoria()).isSameAs(calzadoHombres);
        verifyNoInteractions(imagenService);
        verify(repository).save(actual);
    }

    @Test
    void rechazaModificarUnProductoInexistente() {
        assertThatThrownBy(() -> service.modificarProducto("x", "Remera", "Desc", "M", false, null, "sub-1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El producto no existe o fue eliminado.");
        verify(repository, never()).save(any());
    }

    @Test
    void eliminaLogicamente() throws Exception {
        Producto actual = producto("1", "REM-M", "Remera", "M", false, ropaMujeres);
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        service.eliminarProducto("1");
        assertThat(actual.isEliminado()).isTrue();
        verify(repository).save(actual);
        verify(repository, never()).delete(any());
    }

    @Test
    void buscaPorCodigoYPorNombreSinImportarMayusculasNiTildes() throws Exception {
        Producto zapatilla = producto("1", "ZAP-42", "Zapatilla Montaña", "42", false, calzadoHombres);
        when(repository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(zapatilla));
        assertThat(service.buscarProductoPorCodigo("zap-42")).isSameAs(zapatilla);
        assertThat(service.buscarProductoPorNombre("zapatilla montana")).isSameAs(zapatilla);
        assertThatThrownBy(() -> service.buscarProductoPorCodigo("OTRO"))
                .hasMessage("No existe un producto activo con el código OTRO.");
    }

    @Test
    void filtraPorTextoCategoriaSubcategoriaYOferta() {
        Producto zapatilla = producto("1", "ZAP-42", "Zapatilla Montaña", "42", true, calzadoHombres);
        Producto remera = producto("2", "REM-M", "Remera", "M", false, ropaMujeres);
        when(repository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(zapatilla, remera));
        assertThat(service.listarProductoActivo("montana", null, null, null)).containsExactly(zapatilla);
        assertThat(service.listarProductoActivo("rem-", null, null, null)).containsExactly(remera);
        assertThat(service.listarProductoActivo(null, "cat-2", null, null)).containsExactly(remera);
        assertThat(service.listarProductoActivo("", "", "sub-1", null)).containsExactly(zapatilla);
        assertThat(service.listarProductoActivo(null, null, null, false)).containsExactly(remera);
        assertThat(service.listarProductoActivo(null, null, null, null)).containsExactly(zapatilla, remera);
    }

    @Test
    void paginaLasFilasConElStockActualYPrecioProvisorio() {
        List<Producto> productos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            productos.add(producto(String.valueOf(i), "COD-" + i, "Producto " + i, "M", i == 11, ropaMujeres));
        }
        when(repository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(productos);
        when(stockService.buscarStockActual("11")).thenReturn(7);
        Page<FilaTablaImagenDTO> segunda = service.listarFilaProductoActivo(null, null, null, null, 2, 10);
        assertThat(segunda.getTotalPages()).isEqualTo(2);
        assertThat(segunda.getTotalElements()).isEqualTo(12);
        assertThat(segunda.getNumber()).isEqualTo(1);
        assertThat(segunda.getContent()).containsExactly(
                new FilaTablaImagenDTO("11", "Producto 11 (talle M)", null,
                        List.of("COD-11", "Producto 11", "M", "Mujeres / Ropa", "Sí", "Sin precio", "7")),
                new FilaTablaImagenDTO("12", "Producto 12 (talle M)", null,
                        List.of("COD-12", "Producto 12", "M", "Mujeres / Ropa", "No", "Sin precio", "0")));
        // Una página fuera de rango devuelve la última.
        assertThat(service.listarFilaProductoActivo(null, null, null, null, 9, 10).getNumber()).isEqualTo(1);
    }

    private Producto producto(String id, String codigo, String nombre, String talle, boolean enOferta,
            SubCategoria subCategoria) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setDescripcion("Descripción");
        producto.setTalle(talle);
        producto.setEnOferta(enOferta);
        producto.setSubCategoria(subCategoria);
        return producto;
    }

    private static Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private static SubCategoria subCategoria(String id, String nombre, Categoria categoria) {
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setId(id);
        subCategoria.setNombre(nombre);
        subCategoria.setCategoria(categoria);
        return subCategoria;
    }
}
