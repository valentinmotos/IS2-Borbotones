package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.EmpleadoRepository;
import com.zero.ecommerce.repositories.PersonaRepository;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    private static final LocalDate MAYOR_DE_EDAD = LocalDate.now().minusYears(30);

    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private UsuarioService usuarioService;
    @InjectMocks
    private EmpleadoService service;

    @Test
    void rechazaDocumentoRepetido() {
        Empleado otro = new Empleado();
        otro.setId("otro");
        when(personaRepository.findByTipoDocumentoAndNumeroDocumentoAndEliminadoFalse(TipoDocumento.DNI, "30111222"))
                .thenReturn(List.of(otro));
        assertThatThrownBy(() -> crear(MAYOR_DE_EDAD, "30.111.222"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya hay otra persona registrada con ese documento.");
        verifyNoInteractions(usuarioService);
        verify(empleadoRepository, never()).save(any());
    }

    @Test
    void rechazaDniConFormatoInvalido() {
        assertThatThrownBy(() -> crear(MAYOR_DE_EDAD, "123"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El DNI tiene que tener 7 u 8 números.");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void rechazaEmpleadoMenorDeEdad() {
        assertThatThrownBy(() -> crear(LocalDate.now().minusYears(17), "30111222"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El empleado tiene que ser mayor de edad.");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void exigeNombre() {
        assertThatThrownBy(() -> service.crearEmpleado(" ", "Pérez", MAYOR_DE_EDAD, TipoDocumento.DNI, "30111222",
                "nuevo@zero.com.ar", RolUsuario.ADMINISTRATIVO, "Clave123!", "Clave123!"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El nombre es obligatorio.");
        verifyNoInteractions(usuarioService);
    }

    private Empleado crear(LocalDate fechaNacimiento, String documento) throws ErrorServiceException {
        return service.crearEmpleado("Ana", "Pérez", fechaNacimiento, TipoDocumento.DNI, documento,
                "nuevo@zero.com.ar", RolUsuario.ADMINISTRATIVO, "Clave123!", "Clave123!");
    }
}
