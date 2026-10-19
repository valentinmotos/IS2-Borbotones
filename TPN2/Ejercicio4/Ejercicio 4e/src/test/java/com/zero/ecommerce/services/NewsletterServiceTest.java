package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.NewsletterEnvioResultadoDTO;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.repositories.NewsletterEnvioRepository;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock CatalogoService catalogoService;
    @Mock ClienteService clienteService;
    @Mock EmailService emailService;
    @Mock NewsletterEnvioRepository envioRepository;

    private NewsletterService service;
    private ProductoCatalogoDTO oferta;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        service = new NewsletterService(catalogoService, clienteService, emailService, envioRepository,
                "https://zero.example/");
        oferta = new ProductoCatalogoDTO("prod-1", "REM-1", "Remera Zero", "Descripción", "M",
                "img-1", 19990, "$19.990", true, 4, "Hombres", "Ropa", "sub-1");
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("cliente@zero.test");
        cliente = new Cliente();
        cliente.setNombre("Valen");
        cliente.setUsuario(usuario);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enviaOfertasConUrlsAbsolutasYActualizaLaFecha() throws Exception {
        when(catalogoService.listar(CatalogoFiltro.ofertas())).thenReturn(List.of(oferta));
        when(clienteService.listarClientesActivosConPerfilCompleto()).thenReturn(List.of(cliente));

        NewsletterEnvioResultadoDTO resultado = service.enviar();

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviarSincronico(eq("cliente@zero.test"), eq(NewsletterService.ASUNTO),
                eq("newsletter"), variables.capture());
        assertThat(variables.getValue()).containsEntry("urlBase", "https://zero.example")
                .containsEntry("nombreCliente", "Valen");
        assertThat(variables.getValue().get("ofertas")).isEqualTo(List.of(oferta));
        verify(envioRepository).guardarUltimoEnvio(LocalDate.now());
        assertThat(resultado.cantidadEnviada()).isEqualTo(1);
        assertThat(resultado.proximoEnvio()).isEqualTo(LocalDate.now().plusDays(10));
    }

    @Test
    void sinOfertasNoEnviaNiRegistraLaFecha() throws Exception {
        when(catalogoService.listar(CatalogoFiltro.ofertas())).thenReturn(List.of());

        assertThatThrownBy(service::enviar)
                .hasMessage("No hay ofertas con stock para enviar en el newsletter.");
        verify(emailService, never()).enviarSincronico(any(), any(), any(), any());
        verify(envioRepository, never()).guardarUltimoEnvio(any());
    }

    @Test
    void respetaLosDiezDiasDesdeElUltimoEnvio() {
        when(envioRepository.buscarUltimoEnvio()).thenReturn(Optional.of(LocalDate.now().minusDays(9)));
        assertThat(service.correspondeEnviarHoy()).isFalse();

        when(envioRepository.buscarUltimoEnvio()).thenReturn(Optional.of(LocalDate.now().minusDays(10)));
        assertThat(service.correspondeEnviarHoy()).isTrue();
    }
}
