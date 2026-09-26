package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.ContactoItemDTO;
import com.zero.ecommerce.entities.Contacto;
import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository repository;
    @Mock
    private ContactoService contactoService;
    @InjectMocks
    private ProveedorService service;

    @Test
    void rechazaUnProveedorSinTelefonoCelular() {
        List<ContactoItemDTO> contactos = List.of(correo(null, "ventas@cuyo.com.ar"), fijo(null, "261 423-9870"));
        assertThatThrownBy(() -> service.crearProveedor("Deportiva Cuyo", contactos))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El proveedor tiene que tener al menos un teléfono celular para WhatsApp.");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnProveedorSinCorreo() {
        assertThatThrownBy(() -> service.crearProveedor("Deportiva Cuyo", List.of(celular(null, "5492614123456"))))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El proveedor tiene que tener al menos un correo electrónico.");
    }

    @Test
    void rechazaUnaRazonSocialRepetida() {
        when(repository.findByRazonSocialIgnoreCaseAndEliminadoFalse("Deportiva Cuyo"))
                .thenReturn(Optional.of(proveedor("otro")));
        assertThatThrownBy(() -> service.crearProveedor(" Deportiva Cuyo ", contactosValidos()))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe un proveedor con esa razón social.");
    }

    @Test
    void laRazonSocialPropiaNoCuentaComoRepetidaAlModificar() throws ErrorServiceException {
        Proveedor proveedor = proveedor("p1");
        when(repository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(proveedor));
        when(repository.findByRazonSocialIgnoreCaseAndEliminadoFalse("Deportiva Cuyo")).thenReturn(Optional.of(proveedor));
        service.modificarProveedor("p1", "Deportiva Cuyo", contactosValidos());
        verify(repository).save(proveedor);
    }

    @Test
    void rechazaUnCelularSinCodigoDePais() {
        List<ContactoItemDTO> contactos = List.of(correo(null, "ventas@cuyo.com.ar"), celular(null, "261 412-3456"));
        assertThatThrownBy(() -> service.crearProveedor("Deportiva Cuyo", contactos))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("tiene que estar en formato internacional");
    }

    @Test
    void rechazaUnCelularArgentinoSinEl9() {
        List<ContactoItemDTO> contactos = List.of(correo(null, "ventas@cuyo.com.ar"), celular(null, "542614123456"));
        assertThatThrownBy(() -> service.crearProveedor("Deportiva Cuyo", contactos))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("los celulares de Argentina van con 549");
    }

    @Test
    void rechazaUnTipoDeContactoQueNoExiste() {
        ContactoItemDTO fax = new ContactoItemDTO(null, "FAX", "2614239870", "EMPRESA", null);
        assertThatThrownBy(() -> service.crearProveedor("Deportiva Cuyo", List.of(correo(null, "a@b.com"), fax)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Cada contacto tiene que ser un correo electrónico, un celular o un teléfono fijo.");
    }

    @Test
    void guardaElCelularSoloConDigitosParaWhatsApp() throws ErrorServiceException {
        List<ContactoItemDTO> contactos = List.of(correo(null, "ventas@cuyo.com.ar"), celular(null, "+54 9 261 412-3456"));
        service.crearProveedor("Deportiva Cuyo", contactos);
        verify(contactoService).crearContactoTelefonico("5492614123456", TipoTelefono.CELULAR, TipoContacto.EMPRESA, null);
        verify(contactoService).crearContactoCorreoElectronico("ventas@cuyo.com.ar", TipoContacto.EMPRESA, null);
    }

    @Test
    void modificarReusaLosContactosExistentesYDaDeBajaLosQuitados() throws ErrorServiceException {
        Proveedor proveedor = proveedor("p1");
        proveedor.getContactos().addAll(List.of(contactoCorreo("c1"), contactoTelefono("t1", TipoTelefono.CELULAR),
                contactoTelefono("f1", TipoTelefono.FIJO)));
        when(repository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(proveedor));

        // c1 cambia el correo, t1 queda igual, f1 se quitó y se agrega un correo nuevo.
        service.modificarProveedor("p1", "Deportiva Cuyo", List.of(correo("c1", "compras@cuyo.com.ar"),
                celular("t1", "5492614123456"), correo(null, "nuevo@cuyo.com.ar")));

        verify(contactoService).modificarContactoCorreoElectronico("c1", "compras@cuyo.com.ar", TipoContacto.EMPRESA, null);
        verify(contactoService).modificarContactoTelefonico("t1", "5492614123456", TipoTelefono.CELULAR,
                TipoContacto.EMPRESA, null);
        verify(contactoService).crearContactoCorreoElectronico("nuevo@cuyo.com.ar", TipoContacto.EMPRESA, null);
        verify(contactoService).eliminarContacto("f1");
        verify(contactoService, never()).eliminarContacto("c1");
        verify(contactoService, never()).eliminarContacto("t1");
    }

    @Test
    void unCorreoQuePasaASerCelularSeDaDeBajaYSeCreaOtro() throws ErrorServiceException {
        Proveedor proveedor = proveedor("p1");
        proveedor.getContactos().addAll(List.of(contactoCorreo("c1"), contactoCorreo("c2")));
        when(repository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(proveedor));

        service.modificarProveedor("p1", "Deportiva Cuyo",
                List.of(correo("c1", "ventas@cuyo.com.ar"), celular("c2", "5492614123456")));

        verify(contactoService).eliminarContacto("c2");
        verify(contactoService).crearContactoTelefonico("5492614123456", TipoTelefono.CELULAR, TipoContacto.EMPRESA, null);
        verify(contactoService, never()).modificarContactoTelefonico(eq("c2"), anyString(), any(), any(), any());
    }

    @Test
    void unIdQueNoEsDelProveedorNoModificaEseContacto() throws ErrorServiceException {
        Proveedor proveedor = proveedor("p1");
        when(repository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(proveedor));

        service.modificarProveedor("p1", "Deportiva Cuyo",
                List.of(correo("de-otro", "ventas@cuyo.com.ar"), celular(null, "5492614123456")));

        verify(contactoService, never()).modificarContactoCorreoElectronico(eq("de-otro"), any(), any(), any());
        verify(contactoService).crearContactoCorreoElectronico("ventas@cuyo.com.ar", TipoContacto.EMPRESA, null);
    }

    @Test
    void eliminarDaDeBajaElProveedorYSusContactos() throws ErrorServiceException {
        Proveedor proveedor = proveedor("p1");
        proveedor.getContactos().addAll(List.of(contactoCorreo("c1"), contactoTelefono("t1", TipoTelefono.CELULAR)));
        when(repository.findByIdAndEliminadoFalse("p1")).thenReturn(Optional.of(proveedor));

        service.eliminarProveedor("p1");

        verify(contactoService).eliminarContacto("c1");
        verify(contactoService).eliminarContacto("t1");
        verify(repository).save(proveedor);
        assertThat(proveedor.isEliminado()).isTrue();
    }

    private List<ContactoItemDTO> contactosValidos() {
        return List.of(correo(null, "ventas@cuyo.com.ar"), celular(null, "5492614123456"));
    }

    private ContactoItemDTO correo(String id, String email) {
        return new ContactoItemDTO(id, ContactoItemDTO.CORREO, email, "EMPRESA", null);
    }

    private ContactoItemDTO celular(String id, String telefono) {
        return new ContactoItemDTO(id, ContactoItemDTO.CELULAR, telefono, "EMPRESA", null);
    }

    private ContactoItemDTO fijo(String id, String telefono) {
        return new ContactoItemDTO(id, ContactoItemDTO.FIJO, telefono, "EMPRESA", null);
    }

    private Proveedor proveedor(String id) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setRazonSocial("Deportiva Cuyo");
        return proveedor;
    }

    private Contacto contactoCorreo(String id) {
        ContactoCorreoElectronico correo = new ContactoCorreoElectronico();
        correo.setId(id);
        correo.setEmail(id + "@cuyo.com.ar");
        return correo;
    }

    private Contacto contactoTelefono(String id, TipoTelefono tipo) {
        ContactoTelefonico telefono = new ContactoTelefonico();
        telefono.setId(id);
        telefono.setTelefono("5492614123456");
        telefono.setTipoTelefono(tipo);
        return telefono;
    }
}
