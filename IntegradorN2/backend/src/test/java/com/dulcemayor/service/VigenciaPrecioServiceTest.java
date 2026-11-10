package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.VigenciaPrecioRequest;
import com.dulcemayor.dto.mapper.VigenciaPrecioMapperImpl;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.VigenciaPrecio;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;

@ExtendWith(MockitoExtension.class)
class VigenciaPrecioServiceTest {

    private static final String ID_PRODUCTO = "producto-1";

    @Mock private VigenciaPrecioRepository vigenciaRepository;
    @Mock private ProductoRepository productoRepository;
    private VigenciaPrecioService service;
    private Producto producto;

    @BeforeEach
    void setUp() {
        service = new VigenciaPrecioService(vigenciaRepository, productoRepository, new VigenciaPrecioMapperImpl());
        producto = new Producto();
        producto.setId(ID_PRODUCTO);
    }

    /** Simula la tabla en memoria: lo que se guarda queda disponible para las consultas. */
    private List<VigenciaPrecio> tablaEnMemoria() {
        List<VigenciaPrecio> tabla = new ArrayList<>();
        when(productoRepository.buscarActivoParaActualizar(ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(vigenciaRepository.findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc(
                ID_PRODUCTO)).thenAnswer(inv -> tabla.stream().filter(v -> v.getFechaHasta() == null).findFirst());
        when(vigenciaRepository.save(any(VigenciaPrecio.class))).thenAnswer(inv -> {
            VigenciaPrecio vigencia = inv.getArgument(0);
            if (vigencia.getId() == null) {
                vigencia.setId("vigencia-" + (tabla.size() + 1));
                tabla.add(vigencia);
            }
            return vigencia;
        });
        return tabla;
    }

    @Test
    void tresCambiosDePrecioDejanTresVigenciasSinSuperposicionYElUltimoVigente() {
        List<VigenciaPrecio> tabla = tablaEnMemoria();
        LocalDate hoy = LocalDate.now();

        service.crearVigenciaPrecio(ID_PRODUCTO, new VigenciaPrecioRequest(hoy, 10000.0));
        service.crearVigenciaPrecio(ID_PRODUCTO, new VigenciaPrecioRequest(hoy.plusDays(10), 13000.0));
        var ultima = service.crearVigenciaPrecio(ID_PRODUCTO, new VigenciaPrecioRequest(hoy.plusDays(20), 15000.0));

        assertThat(tabla).hasSize(3);
        List<VigenciaPrecio> ordenadas = tabla.stream()
                .sorted(Comparator.comparing(VigenciaPrecio::getFechaDesde)).toList();
        for (int i = 0; i < ordenadas.size() - 1; i++) {
            VigenciaPrecio actual = ordenadas.get(i);
            VigenciaPrecio siguiente = ordenadas.get(i + 1);
            assertThat(actual.getFechaHasta()).isEqualTo(siguiente.getFechaDesde().minusDays(1));
            assertThat(actual.getFechaHasta()).isAfterOrEqualTo(actual.getFechaDesde());
        }
        assertThat(tabla).filteredOn(v -> v.getFechaHasta() == null).hasSize(1);
        assertThat(ultima.vigente()).isTrue();
        assertThat(ultima.precio()).isEqualTo(15000.0);
    }

    @Test
    void primerPrecioQuedaAbierto() {
        tablaEnMemoria();

        var respuesta = service.crearVigenciaPrecio(ID_PRODUCTO,
                new VigenciaPrecioRequest(LocalDate.now(), 5000.0));

        assertThat(respuesta.fechaHasta()).isNull();
        assertThat(respuesta.vigente()).isTrue();
        assertThat(respuesta.productoId()).isEqualTo(ID_PRODUCTO);
    }

    @Test
    void rechazaFechaIgualAlInicioDelVigente() {
        tablaEnMemoria();
        LocalDate hoy = LocalDate.now();
        service.crearVigenciaPrecio(ID_PRODUCTO, new VigenciaPrecioRequest(hoy, 5000.0));

        assertThatThrownBy(() -> service.crearVigenciaPrecio(ID_PRODUCTO, new VigenciaPrecioRequest(hoy, 6000.0)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageStartingWith("La fecha desde debe ser posterior al inicio del precio vigente")
                .extracting("campo").isEqualTo("fechaDesde");
    }

    @Test
    void rechazaFechaAnteriorAHoy() {
        when(productoRepository.buscarActivoParaActualizar(ID_PRODUCTO)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearVigenciaPrecio(ID_PRODUCTO,
                new VigenciaPrecioRequest(LocalDate.now().minusDays(1), 5000.0)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La fecha desde no puede ser anterior a hoy.");
        verify(vigenciaRepository, never()).save(any());
    }

    @Test
    void rechazaPrecioNoPositivo() {
        when(productoRepository.buscarActivoParaActualizar(ID_PRODUCTO)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearVigenciaPrecio(ID_PRODUCTO,
                new VigenciaPrecioRequest(LocalDate.now(), 0.0)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El precio debe ser mayor que cero.");
        verify(vigenciaRepository, never()).save(any());
    }

    @Test
    void productoInexistenteDa404() {
        when(productoRepository.buscarActivoParaActualizar("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearVigenciaPrecio("otro",
                new VigenciaPrecioRequest(LocalDate.now(), 5000.0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el producto.");
    }

    @Test
    void productoSinPrecioNoTieneVigente() {
        when(productoRepository.findByIdAndEliminadoFalse(ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(vigenciaRepository.findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc(
                ID_PRODUCTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarVigenciaPrecioVigente(ID_PRODUCTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El producto no tiene un precio vigente.");
        assertThat(service.buscarPrecioVigente(ID_PRODUCTO)).isEmpty();
    }
}
