package com.borbotones.integrador1;

import com.borbotones.integrador1.entities.Categoria;
import com.borbotones.integrador1.repositories.CategoriaRepository;
import com.borbotones.integrador1.services.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CategoriaServiceTest {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void setUp() {
        categoriaRepository.deleteAll();
    }

    @Test
    @DisplayName("crearCategoria con nombre válido debe persistir la categoría activa")
    void crearCategoriaValida() {
        categoriaService.crearCategoria("Hombres");

        Categoria encontrada = categoriaService.buscarCategoriaPorNombre("Hombres");
        assertNotNull(encontrada.getId());
        assertEquals("Hombres", encontrada.getNombre());
        assertFalse(encontrada.isEliminado(), "La categoría recién creada debe tener eliminado = false");
    }

    @Test
    @DisplayName("crearCategoria con nombre vacío o nulo debe lanzar IllegalArgumentException")
    void crearCategoriaNombreInvalido() {
        assertThrows(IllegalArgumentException.class, () -> categoriaService.crearCategoria(null));
        assertThrows(IllegalArgumentException.class, () -> categoriaService.crearCategoria(""));
        assertThrows(IllegalArgumentException.class, () -> categoriaService.crearCategoria("   "));
        assertThrows(IllegalArgumentException.class, () -> categoriaService.crearCategoria("A")); // menor a 2 caracteres
    }

    @Test
    @DisplayName("crearCategoria con nombre duplicado debe lanzar IllegalArgumentException")
    void crearCategoriaDuplicada() {
        categoriaService.crearCategoria("Mujeres");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            categoriaService.crearCategoria("mujeres");
        });
        assertTrue(ex.getMessage().contains("Ya existe una categoría"));
    }

    @Test
    @DisplayName("buscarCategoria por ID debe retornar la entidad o lanzar excepción si no existe")
    void buscarCategoriaPorId() {
        categoriaService.crearCategoria("Niños");
        Categoria creada = categoriaService.buscarCategoriaPorNombre("Niños");

        Categoria encontrada = categoriaService.buscarCategoria(creada.getId());
        assertEquals(creada.getId(), encontrada.getId());

        assertThrows(NoSuchElementException.class, () -> categoriaService.buscarCategoria("id-inexistente-123"));
    }

    @Test
    @DisplayName("modificarCategoria debe actualizar el nombre de la categoría")
    void modificarCategoriaExito() {
        categoriaService.crearCategoria("Calzado");
        Categoria creada = categoriaService.buscarCategoriaPorNombre("Calzado");

        categoriaService.modificarCategoria(creada.getId(), "Calzado Deportivo");

        Categoria modificada = categoriaService.buscarCategoria(creada.getId());
        assertEquals("Calzado Deportivo", modificada.getNombre());
    }

    @Test
    @DisplayName("modificarCategoria a un nombre ya existente en otra categoría debe lanzar excepción")
    void modificarCategoriaNombreDuplicado() {
        categoriaService.crearCategoria("Ropa");
        categoriaService.crearCategoria("Accesorios");
        Categoria accesorios = categoriaService.buscarCategoriaPorNombre("Accesorios");

        assertThrows(IllegalArgumentException.class, () -> {
            categoriaService.modificarCategoria(accesorios.getId(), "ropa");
        });
    }

    @Test
    @DisplayName("eliminarCategoria debe realizar baja lógica marcando eliminado = true")
    void eliminarCategoriaBajaLogica() {
        categoriaService.crearCategoria("Temporal");
        Categoria creada = categoriaService.buscarCategoriaPorNombre("Temporal");

        categoriaService.eliminarCategoria(creada.getId());

        Categoria dadaDeBaja = categoriaService.buscarCategoria(creada.getId());
        assertTrue(dadaDeBaja.isEliminado(), "La baja debe ser lógica (eliminado = true)");
        assertNotNull(categoriaRepository.findById(creada.getId()).orElse(null), "El registro no debe eliminarse físicamente de la base de datos");
    }

    @Test
    @DisplayName("listarCategoriaActivo debe omitir categorías dadas de baja lógica")
    void listarCategoriaActivoFiltraEliminadas() {
        categoriaService.crearCategoria("Activa Uno");
        categoriaService.crearCategoria("Activa Dos");
        categoriaService.crearCategoria("Para Eliminar");

        Categoria paraEliminar = categoriaService.buscarCategoriaPorNombre("Para Eliminar");
        categoriaService.eliminarCategoria(paraEliminar.getId());

        Collection<Categoria> activas = categoriaService.listarCategoriaActivo();
        Collection<Categoria> todas = categoriaService.listarCategoria();

        assertEquals(2, activas.size(), "Solo deben listarse las 2 categorías activas");
        assertEquals(3, todas.size(), "En el listado total deben figurar las 3");
        assertTrue(activas.stream().noneMatch(Categoria::isEliminado), "Ninguna categoría activa debe tener eliminado = true");
    }

    @Test
    @DisplayName("reactivarCategoria debe restablecer eliminado = false")
    void reactivarCategoriaExito() {
        categoriaService.crearCategoria("Archivada");
        Categoria cat = categoriaService.buscarCategoriaPorNombre("Archivada");
        categoriaService.eliminarCategoria(cat.getId());

        categoriaService.reactivarCategoria(cat.getId());

        Categoria reactivada = categoriaService.buscarCategoria(cat.getId());
        assertFalse(reactivada.isEliminado());
    }
}
