package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.dto.ActualizacionPrecioDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;

@ExtendWith(MockitoExtension.class)
class VigenciaPrecioServiceTest {

    @Mock
    private VigenciaPrecioRepository repository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SubCategoriaRepository subCategoriaRepository;

    @InjectMocks
    private VigenciaPrecioService service;

    @Test
    void creaUnaNuevaVigenciaYCierraLaAnterior() throws Exception {
        Producto producto = new Producto();
        producto.setId("prod-1");
        when(productoRepository.existsByIdAndEliminadoFalse("prod-1")).thenReturn(true);
        when(productoRepository.findByIdAndEliminadoFalse("prod-1")).thenReturn(Optional.of(producto));

        VigenciaPrecio actual = new VigenciaPrecio();
        actual.setId("vig-1");
        actual.setProducto(producto);
        actual.setFechaDesde(LocalDate.now().minusDays(10));
        actual.setPrecio(1000);
        when(repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc("prod-1"))
                .thenReturn(List.of(actual));

        service.crearVigenciaPrecio("prod-1", LocalDate.now(), 1500);

        ArgumentCaptor<VigenciaPrecio> captor = ArgumentCaptor.forClass(VigenciaPrecio.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<VigenciaPrecio> guardadas = captor.getAllValues();
        assertThat(guardadas).hasSize(2);
        assertThat(guardadas.get(0).getFechaHasta()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(guardadas.get(1).getFechaDesde()).isEqualTo(LocalDate.now());
        assertThat(guardadas.get(1).getFechaHasta()).isNull();
        assertThat(guardadas.get(1).getPrecio()).isEqualTo(1500);
    }

    @Test
    void validaPrecioYFechaAntesDeGuardar() {
        assertThatThrownBy(() -> service.crearVigenciaPrecio("prod-1", LocalDate.now().minusDays(1), 0))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El precio de la vigencia debe ser mayor a 0.");
        assertThatThrownBy(() -> service.crearVigenciaPrecio("prod-1", LocalDate.now().minusDays(2), 1500))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde de la vigencia no puede ser anterior a hoy.");
        verify(repository, never()).save(any());
    }

    @Test
    void previsualizaSoloLaCategoriaElegidaYRedondeaAMultiplosDeDiez() throws Exception {
        Categoria hombres = categoria("cat-h", "Hombres");
        Producto zapatilla = producto("prod-1", "Zapatilla Run", "Zapatilla", "42", hombres);
        Producto remera = producto("prod-2", "Remera Zero", "Remera", "M", hombres);
        Producto calza = producto("prod-3", "Calza Fit", "Calza", "S", categoria("cat-m", "Mujeres"));
        when(categoriaRepository.findByIdAndEliminadoFalse("cat-h")).thenReturn(Optional.of(hombres));
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc())
                .thenReturn(List.of(zapatilla, remera, calza));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull()).thenReturn(List.of(
                vigencia(zapatilla, 1001), vigencia(remera, 2000), vigencia(calza, 500)));

        var preview = service.previsualizarActualizacionMasiva("CATEGORIA", "cat-h", 15, LocalDate.now());

        assertThat(preview).hasSize(2);
        assertThat(preview).extracting(ActualizacionPrecioDTO::precioNuevo).containsExactly(1150.0, 2300.0);
        assertThat(preview).extracting(ActualizacionPrecioDTO::diferencia).containsExactly(149.0, 300.0);
        assertThat(service.previsualizarActualizacionMasiva("TODO", null, 15, LocalDate.now())).hasSize(3);
    }

    @Test
    void previsualizaSoloLaSubcategoriaElegida() throws Exception {
        Categoria hombres = categoria("cat-h", "Hombres");
        Producto zapatilla = producto("prod-1", "Zapatilla Run", "Zapatilla", "42", hombres);
        Producto remera = producto("prod-2", "Remera Zero", "Remera", "M", hombres);
        SubCategoria calzado = zapatilla.getSubCategoria();
        when(subCategoriaRepository.findByIdAndEliminadoFalse(calzado.getId())).thenReturn(Optional.of(calzado));
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(zapatilla, remera));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull())
                .thenReturn(List.of(vigencia(zapatilla, 1000), vigencia(remera, 2000)));

        var preview = service.previsualizarActualizacionMasiva("SUBCATEGORIA", calzado.getId(), 10,
                LocalDate.now());

        assertThat(preview).extracting(ActualizacionPrecioDTO::productoId).containsExactly("prod-1");
    }

    @Test
    void noPrevisualizaNiGuardaSiUnProductoNoTienePrecioVigente() {
        Producto producto = producto("prod-1", "Producto sin precio", "Producto", "U",
                categoria("cat-h", "Hombres"));
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(producto));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull()).thenReturn(List.of());

        assertThatThrownBy(() -> service.actualizarPreciosMasivo("TODO", null, 15, LocalDate.now()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no tiene un precio vigente");

        verify(repository, never()).save(any());
    }

    @Test
    void rechazaParametrosInvalidosEnLaActualizacionMasiva() {
        assertThatThrownBy(() -> service.previsualizarActualizacionMasiva("TODO", null, 0, LocalDate.now()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El porcentaje de aumento debe ser mayor a 0.");
        assertThatThrownBy(() -> service.previsualizarActualizacionMasiva("TODO", null, 10,
                LocalDate.now().minusDays(1)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde de la actualización no puede ser anterior a hoy.");
        assertThatThrownBy(() -> service.previsualizarActualizacionMasiva("DESCONOCIDO", null, 10,
                LocalDate.now()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El alcance de la actualización no es válido.");

        verify(repository, never()).save(any());
    }

    @Test
    void evitaCrearUnHistorialInvalidoSiElPrecioVigenteComenzoEseMismoDia() {
        Producto producto = producto("prod-1", "ZAP-42", "Zapatilla Run", "42",
                categoria("cat-h", "Hombres"));
        VigenciaPrecio vigente = vigencia(producto, 1000);
        vigente.setFechaDesde(LocalDate.now());
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(producto));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull()).thenReturn(List.of(vigente));

        assertThatThrownBy(() -> service.previsualizarActualizacionMasiva("TODO", null, 15, LocalDate.now()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde debe ser posterior al inicio del precio vigente de Zapatilla Run.");

        verify(repository, never()).save(any());
    }

    @Test
    void confirmarRechazaUnaVistaPreviaDesactualizada() throws Exception {
        Producto producto = producto("prod-1", "ZAP-42", "Zapatilla Run", "42",
                categoria("cat-h", "Hombres"));
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc()).thenReturn(List.of(producto));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull()).thenReturn(List.of(vigencia(producto, 1200)));
        List<ActualizacionPrecioDTO> vistaPreviaAnterior = List.of(
                new ActualizacionPrecioDTO("prod-1", "ZAP-42", "Zapatilla Run", "42", 1000, 1150, 150));

        assertThatThrownBy(() -> service.confirmarActualizacionMasiva("TODO", null, 15, LocalDate.now(),
                vistaPreviaAnterior))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Los precios cambiaron desde la vista previa. Revisá los valores antes de confirmar.");

        verify(repository, never()).save(any());
    }

    @Test
    void actualizaSoloLosProductosDeLaCategoriaYGuardaLosPreciosDelPreview() throws Exception {
        Categoria hombres = categoria("cat-h", "Hombres");
        Producto zapatilla = producto("prod-1", "ZAP-42", "Zapatilla Run", "42", hombres);
        Producto remera = producto("prod-2", "REM-M", "Remera Zero", "M", hombres);
        Producto calza = producto("prod-3", "CAL-S", "Calza Fit", "S", categoria("cat-m", "Mujeres"));
        VigenciaPrecio precioZapatilla = vigencia(zapatilla, 1001);
        VigenciaPrecio precioRemera = vigencia(remera, 2000);
        when(categoriaRepository.findByIdAndEliminadoFalse("cat-h")).thenReturn(Optional.of(hombres));
        when(productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc())
            .thenReturn(List.of(zapatilla, remera, calza));
        when(repository.findByEliminadoFalseAndFechaHastaIsNull())
            .thenReturn(List.of(precioZapatilla, precioRemera, vigencia(calza, 500)));
        when(productoRepository.existsByIdAndEliminadoFalse("prod-1")).thenReturn(true);
        when(productoRepository.existsByIdAndEliminadoFalse("prod-2")).thenReturn(true);
        when(productoRepository.findByIdAndEliminadoFalse("prod-1")).thenReturn(Optional.of(zapatilla));
        when(productoRepository.findByIdAndEliminadoFalse("prod-2")).thenReturn(Optional.of(remera));
        when(repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc("prod-1"))
            .thenReturn(List.of(precioZapatilla));
        when(repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc("prod-2"))
            .thenReturn(List.of(precioRemera));

        int actualizados = service.actualizarPreciosMasivo("CATEGORIA", "cat-h", 15, LocalDate.now());

        assertThat(actualizados).isEqualTo(2);
        ArgumentCaptor<VigenciaPrecio> guardadas = ArgumentCaptor.forClass(VigenciaPrecio.class);
        verify(repository, org.mockito.Mockito.times(4)).save(guardadas.capture());
        assertThat(guardadas.getAllValues()).filteredOn(vigencia -> vigencia.getFechaHasta() == null)
            .extracting(VigenciaPrecio::getPrecio).containsExactly(1150.0, 2300.0);
        verify(productoRepository, never()).findByIdAndEliminadoFalse("prod-3");
    }

    private Producto producto(String id, String codigo, String nombre, String talle, Categoria categoria) {
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setId("sub-" + id);
        subCategoria.setNombre("Subcategoría");
        subCategoria.setCategoria(categoria);
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setTalle(talle);
        producto.setSubCategoria(subCategoria);
        return producto;
    }

    private Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private VigenciaPrecio vigencia(Producto producto, double precio) {
        VigenciaPrecio vigencia = new VigenciaPrecio();
        vigencia.setProducto(producto);
        vigencia.setPrecio(precio);
        vigencia.setFechaDesde(LocalDate.now().minusDays(30));
        return vigencia;
    }
}
