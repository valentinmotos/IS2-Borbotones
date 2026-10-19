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
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;

@ExtendWith(MockitoExtension.class)
class SubCategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private SubCategoriaRepository subCategoriaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @InjectMocks
    private SubCategoriaService service;

    @Test
    void creaSubCategoriaValida() throws Exception {
        Categoria categoria = categoria("cat-1", "Ropa");
        when(categoriaRepository.findByIdAndEliminadoFalse("cat-1")).thenReturn(Optional.of(categoria));
        when(subCategoriaRepository.findByCategoria_IdAndNombreIgnoreCaseAndEliminadoFalse("cat-1", "Zapatillas")).thenReturn(Optional.empty());

        service.crearSubCategoria("cat-1", "  Zapatillas  ");

        ArgumentCaptor<SubCategoria> captor = ArgumentCaptor.forClass(SubCategoria.class);
        verify(subCategoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Zapatillas");
        assertThat(captor.getValue().getCategoria().getId()).isEqualTo("cat-1");
    }

    @Test
    void rechazaSubCategoriaDuplicadaDentroDeLaCategoria() {
        Categoria categoria = categoria("cat-1", "Ropa");
        when(categoriaRepository.findByIdAndEliminadoFalse("cat-1")).thenReturn(Optional.of(categoria));
        when(subCategoriaRepository.findByCategoria_IdAndNombreIgnoreCaseAndEliminadoFalse("cat-1", "Zapatillas"))
                .thenReturn(Optional.of(subCategoria("sub-1", "Zapatillas", categoria)));

        assertThatThrownBy(() -> service.crearSubCategoria("cat-1", "Zapatillas"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una subcategoría con ese nombre dentro de la categoría seleccionada.");
        verify(subCategoriaRepository, never()).save(any());
    }

    @Test
    void validaCategoriaPadre() {
        when(categoriaRepository.findByIdAndEliminadoFalse("cat-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearSubCategoria("cat-404", "Zapatillas"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La categoría seleccionada no existe o no es válida.");
        verify(subCategoriaRepository, never()).save(any());
    }

    @Test
    void impideEliminarSubCategoriaConProductosActivos() {
        SubCategoria subCategoria = subCategoria("sub-1", "Zapatillas", categoria("cat-1", "Ropa"));
        when(subCategoriaRepository.findByIdAndEliminadoFalse("sub-1")).thenReturn(Optional.of(subCategoria));
        when(productoRepository.existsBySubCategoria_IdAndEliminadoFalse("sub-1")).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarSubCategoria("sub-1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede eliminar la subcategoría porque tiene productos activos.");
        verify(subCategoriaRepository, never()).save(any());
    }

    private Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private SubCategoria subCategoria(String id, String nombre, Categoria categoria) {
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setId(id);
        subCategoria.setNombre(nombre);
        subCategoria.setCategoria(categoria);
        return subCategoria;
    }
}
