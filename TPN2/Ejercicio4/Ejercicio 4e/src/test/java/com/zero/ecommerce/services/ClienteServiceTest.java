package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.entities.enums.Sexo;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.repositories.ClienteRepository;
import com.zero.ecommerce.repositories.PersonaRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private NacionalidadService nacionalidadService;
    @Mock
    private DireccionService direccionService;
    @Mock
    private ContactoService contactoService;
    @Mock
    private ImagenService imagenService;
    @InjectMocks
    private ClienteService service;

    @Test
    void perfilIncompletoSiNoHayCliente() {
        when(repository.findByUsuario_IdAndEliminadoFalse("1")).thenReturn(Optional.empty());
        assertThat(service.perfilCompleto("1")).isFalse();
    }

    @Test
    void perfilIncompletoSinDireccion() {
        Cliente cliente = clienteCompleto();
        cliente.setDireccion(null);
        when(repository.findByUsuario_IdAndEliminadoFalse("1")).thenReturn(Optional.of(cliente));
        assertThat(service.perfilCompleto("1")).isFalse();
    }

    @Test
    void perfilIncompletoSinTelefono() {
        Cliente cliente = clienteCompleto();
        cliente.setTelefono(null);
        when(repository.findByUsuario_IdAndEliminadoFalse("1")).thenReturn(Optional.of(cliente));
        assertThat(service.perfilCompleto("1")).isFalse();
    }

    @Test
    void perfilCompletoConDatosDireccionYTelefono() {
        when(repository.findByUsuario_IdAndEliminadoFalse("1")).thenReturn(Optional.of(clienteCompleto()));
        assertThat(service.perfilCompleto("1")).isTrue();
    }

    private Cliente clienteCompleto() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana");
        cliente.setApellido("Pérez");
        cliente.setSexo(Sexo.FEMENINO);
        cliente.setFechaNacimiento(LocalDate.now().minusYears(30));
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");
        cliente.setNacionalidad(new Nacionalidad());
        cliente.setDireccion(new Direccion());
        cliente.setTelefono(new ContactoTelefonico());
        return cliente;
    }
}
