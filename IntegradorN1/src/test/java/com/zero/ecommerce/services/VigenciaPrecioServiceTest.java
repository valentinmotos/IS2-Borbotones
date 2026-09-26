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
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;

@ExtendWith(MockitoExtension.class)
class VigenciaPrecioServiceTest {

    @Mock
    private VigenciaPrecioRepository repository;

    @Mock
    private ProductoRepository productoRepository;

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
}
