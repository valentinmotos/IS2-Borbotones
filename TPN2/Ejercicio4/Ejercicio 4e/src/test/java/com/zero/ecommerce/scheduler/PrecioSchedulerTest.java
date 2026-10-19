package com.zero.ecommerce.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.services.VigenciaPrecioService;

@ExtendWith(MockitoExtension.class)
class PrecioSchedulerTest {

    @Mock
    private VigenciaPrecioService vigenciaPrecioService;

    @InjectMocks
    private PrecioScheduler scheduler;

    @Test
    void ejecutaLaDeteccionDiaria() {
        when(vigenciaPrecioService.listarProductosConPrecioVencido()).thenReturn(List.of());

        scheduler.detectarPreciosVencidos();

        verify(vigenciaPrecioService).listarProductosConPrecioVencido();
    }
}
