package com.borbotones.videojuegos;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.repositories.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuditoriaEnversTests {
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void registraYConsultaElHistorialDeUnaCategoria() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        AtomicLong id = new AtomicLong();

        transaccion.executeWithoutResult(estado -> {
            Categoria categoria = new Categoria();
            categoria.setNombre("Accion");
            categoria.setActivo(true);
            id.set(categoriaRepository.save(categoria).getId());
        });

        transaccion.executeWithoutResult(estado -> {
            Categoria categoria = categoriaRepository.findById(id.get()).orElseThrow();
            categoria.setNombre("Accion y aventura");
            categoriaRepository.save(categoria);
        });

        long cantidadRevisiones = categoriaRepository.findRevisions(id.get()).stream().count();
        assertTrue(cantidadRevisiones >= 2);
        assertEquals(
                "Accion y aventura",
                categoriaRepository.findLastChangeRevision(id.get()).orElseThrow().getEntity().getNombre()
        );
    }
}
