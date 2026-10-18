package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private SubCategoriaRepository subCategoriaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @InjectMocks
    private CategoriaService service;

    @Test
    void creaCategoriaValida() throws Exception {
        when(categoriaRepository.findByNombreIgnoreCaseAndEliminadoFalse("Ropa")).thenReturn(Optional.empty());

        service.crearCategoria("  Ropa  ");

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ropa");
        assertThat(captor.getValue().isEliminado()).isFalse();
    }

    @Test
    void rechazaCategoriaDuplicada() {
        when(categoriaRepository.findByNombreIgnoreCaseAndEliminadoFalse("Ropa")).thenReturn(Optional.of(categoria("1", "Ropa")));

        assertThatThrownBy(() -> service.crearCategoria("Ropa"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una categoría con ese nombre.");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void impideEliminarCategoriaConProductosActivos() {
        Categoria categoria = categoria("1", "Ropa");
        when(categoriaRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(categoria));
        when(productoRepository.existsBySubCategoria_Categoria_IdAndEliminadoFalse("1")).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarCategoria("1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede eliminar la categoría porque tiene productos activos.");
        verify(categoriaRepository, never()).save(any());
    }

    private Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }
}
