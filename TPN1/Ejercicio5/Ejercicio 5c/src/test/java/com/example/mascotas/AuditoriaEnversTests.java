package com.example.mascotas;

import com.example.mascotas.entidades.Zona;
import com.example.mascotas.repositorios.ZonaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuditoriaEnversTests {
    @Autowired
    private ZonaRepositorio zonaRepositorio;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void registraYConsultaElHistorialDeUnaZona() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        AtomicReference<String> id = new AtomicReference<>();

        transaccion.executeWithoutResult(estado -> {
            Zona zona = new Zona();
            zona.setNombre("Centro");
            zona.setDescripcion("Zona inicial");
            id.set(zonaRepositorio.save(zona).getId());
        });

        transaccion.executeWithoutResult(estado -> {
            Zona zona = zonaRepositorio.findById(id.get()).orElseThrow();
            zona.setDescripcion("Zona actualizada");
            zonaRepositorio.save(zona);
        });

        long cantidadRevisiones = zonaRepositorio.findRevisions(id.get()).stream().count();
        assertTrue(cantidadRevisiones >= 2);
        assertEquals(
                "Zona actualizada",
                zonaRepositorio.findLastChangeRevision(id.get()).orElseThrow().getEntity().getDescripcion()
        );
    }
}
