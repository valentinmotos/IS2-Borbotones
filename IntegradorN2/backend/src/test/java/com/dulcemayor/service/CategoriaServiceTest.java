package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.dto.mapper.CategoriaMapperImpl;
import com.dulcemayor.entity.Categoria;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;

/** E0-08: reglas del ABM de referencia sobre la plantilla AbstractCrudService. */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        // Se usa el mapper real que genera MapStruct
        service = new CategoriaService(categoriaRepository, new CategoriaMapperImpl(), productoRepository);
    }

    private static Categoria categoria(String id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    @Test
    void crearGuardaElNombreSinEspaciosYDevuelveElDto() {
        when(categoriaRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocacion -> {
            Categoria guardada = invocacion.getArgument(0);
            guardada.setId("id-1");
            return guardada;
        });

        CategoriaResponse respuesta = service.crearCategoria(new CategoriaRequest("  Alfajores  "));

        assertThat(respuesta).isEqualTo(new CategoriaResponse("id-1", "Alfajores"));
    }

    @Test
    void crearConNombreDuplicadoSinImportarMayusculasNiTildesSeRechaza() {
        when(categoriaRepository.findByEliminadoFalse()).thenReturn(List.of(categoria("id-1", "Golosinas")));

        assertThatThrownBy(() -> service.crearCategoria(new CategoriaRequest("GOLOSÍNAS")))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("Ya existe una categoría con ese nombre.")
                .extracting(ex -> ((DuplicadoException) ex).getCampo()).isEqualTo("nombre");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void crearSinNombreSeRechaza() {
        assertThatThrownBy(() -> service.crearCategoria(new CategoriaRequest("   ")))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El nombre de la categoría es obligatorio.");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void modificarConservandoSuPropioNombreEsValido() {
        Categoria existente = categoria("id-1", "Chocolates");
        when(categoriaRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByEliminadoFalse()).thenReturn(List.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponse respuesta = service.modificarCategoria("id-1", new CategoriaRequest("chocolates"));

        assertThat(respuesta.nombre()).isEqualTo("chocolates");
        assertThat(existente.getId()).isEqualTo("id-1");
    }

    @Test
    void modificarConElNombreDeOtraCategoriaSeRechaza() {
        Categoria existente = categoria("id-1", "Chocolates");
        when(categoriaRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByEliminadoFalse())
                .thenReturn(List.of(existente, categoria("id-2", "Caramelos")));

        assertThatThrownBy(() -> service.modificarCategoria("id-1", new CategoriaRequest("Caramelos")))
                .isInstanceOf(DuplicadoException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminarHaceBajaLogica() {
        Categoria existente = categoria("id-1", "Chicles");
        when(categoriaRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCategoria_IdAndEliminadoFalse("id-1")).thenReturn(false);

        service.eliminarCategoria("id-1");

        assertThat(existente.isEliminado()).isTrue();
        verify(categoriaRepository).save(existente);
    }

    @Test
    void eliminarConProductosActivosSeRechaza() {
        Categoria existente = categoria("id-1", "Chicles");
        when(categoriaRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCategoria_IdAndEliminadoFalse("id-1")).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarCategoria("id-1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede dar de baja la categoría porque tiene productos activos.");
        assertThat(existente.isEliminado()).isFalse();
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void buscarInexistenteLanzaNoEncontrado() {
        when(categoriaRepository.findByIdAndEliminadoFalse("nada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarCategoria("nada"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la categoría.");
    }
}
